package com.smartats.backend

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.smartats.backend.domain.Resume
import com.smartats.backend.domain.User
import com.smartats.backend.domain.UserRole
import com.smartats.backend.dto.job.CreateJobRequest
import com.smartats.backend.repository.JobRecommendationRepository
import com.smartats.backend.repository.JobRepository
import com.smartats.backend.repository.ResumeRepository
import com.smartats.backend.repository.UserRepository
import com.smartats.backend.service.JobService
import com.smartats.backend.service.RecommendationService
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.OutputStream
import java.net.InetSocketAddress
import kotlin.math.sqrt

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class RecommendationLocalEmbeddingIntegrationTest {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var jobRepository: JobRepository

    @Autowired
    private lateinit var resumeRepository: ResumeRepository

    @Autowired
    private lateinit var jobRecommendationRepository: JobRecommendationRepository

    @Autowired
    private lateinit var jobService: JobService

    @Autowired
    private lateinit var recommendationService: RecommendationService

    @BeforeEach
    fun setUp() {
        jobRecommendationRepository.deleteAll()
        jobRepository.deleteAll()
        resumeRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `non native vector mode keeps job creation and matching working on postgres`() {
        val hr = userRepository.save(
            User(
                username = "fallback_hr",
                passwordHash = "noop",
                email = "fallback_hr@example.com",
                role = UserRole.HR,
            ),
        )
        val candidate = userRepository.save(
            User(
                username = "fallback_candidate",
                passwordHash = "noop",
                email = "fallback_candidate@example.com",
                role = UserRole.CANDIDATE,
            ),
        )

        resumeRepository.save(
            Resume(
                candidateName = "Fallback Candidate",
                contactInfo = "fallback_candidate@example.com",
                rawContentReference = "s3://resumes/fallback-candidate.pdf",
                parsedData = sampleParsedData(
                    fullName = "Fallback Candidate",
                    summary = "Build Kotlin Spring Boot recruitment services with PostgreSQL",
                    skills = listOf("Kotlin", "Spring Boot", "PostgreSQL"),
                ),
                ownerUser = candidate,
                status = "PARSED",
            ),
        )

        val jobResponse = jobService.createJob(
            hr.username,
            CreateJobRequest(
                title = "Kotlin Platform Engineer",
                description = "Build Kotlin Spring Boot platform services with PostgreSQL and Redis",
                requirements = mapOf("skills" to listOf("Kotlin", "Spring Boot", "PostgreSQL", "Redis")),
            ),
        )

        assertNull(jobRepository.findById(jobResponse.id).orElseThrow().embedding)

        val evaluation = recommendationService.generateRecommendationsForJob(jobResponse.id)

        assertEquals(1, evaluation.evaluatedCount)
        assertEquals(1, evaluation.recommendations.size)
        assertEquals("Fallback Candidate", evaluation.recommendations.first().candidate.basicInfo?.fullName)
        assertTrue(evaluation.recommendations.first().matchScore > java.math.BigDecimal.ZERO)
        assertEquals(1, jobRecommendationRepository.findByJobId(jobResponse.id).size)
        assertNull(jobRepository.findById(jobResponse.id).orElseThrow().embedding)
        val storedResume = resumeRepository.findTopByOwnerUserIdAndStatusOrderByUpdatedAtDesc(requireNotNull(candidate.id), "PARSED")
        assertNull(storedResume?.embedding)
    }

    private fun sampleParsedData(
        fullName: String,
        summary: String,
        skills: List<String>,
    ): Map<String, Any> {
        return mapOf(
            "basicInfo" to mapOf(
                "fullName" to fullName,
                "email" to "candidate@example.com",
                "phone" to "13800000000",
                "location" to "Shanghai",
                "headline" to "Software Engineer",
                "summary" to summary,
            ),
            "workExperiences" to listOf(
                mapOf(
                    "company" to "Smart ATS",
                    "title" to "Engineer",
                    "startDate" to "2021-01",
                    "endDate" to "2024-12",
                    "responsibilities" to listOf("Build distributed services", "Optimize database access"),
                    "achievements" to listOf("Improved search quality"),
                ),
            ),
            "educationExperiences" to listOf(
                mapOf(
                    "school" to "Tongji University",
                    "degree" to "Bachelor",
                    "fieldOfStudy" to "Computer Science",
                    "startDate" to "2016-09",
                    "endDate" to "2020-06",
                ),
            ),
            "skills" to skills.map { skillName ->
                mapOf(
                    "name" to skillName,
                    "category" to "technical",
                    "proficiency" to "advanced",
                    "evidence" to "Used in production",
                )
            },
            "radarScores" to mapOf(
                "communication" to 8,
                "technicalDepth" to 9,
                "problemSolving" to 9,
                "collaboration" to 8,
                "leadership" to 7,
                "adaptability" to 8,
            ),
            "xaiReasoning" to "Structured candidate profile",
        )
    }

    companion object {
        private val mapper = jacksonObjectMapper()

        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer<Nothing>("pgvector/pgvector:pg17")
            .apply {
                withDatabaseName("smart_ats_local_embedding_test")
                withUsername("postgres")
                withPassword("postgres")
            }

        @JvmStatic
        val embeddingServer: HttpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 18083), 0).apply {
            createContext("/api/embeddings") { exchange ->
                val requestBody = exchange.requestBody.bufferedReader().use { it.readText() }
                val text = mapper.readTree(requestBody).path("text").asText("")
                val vector = buildVector(text)
                val payload = mapper.writeValueAsBytes(mapOf("embedding" to vector, "dimensions" to vector.size))
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(200, payload.size.toLong())
                exchange.responseBody.use { output: OutputStream -> output.write(payload) }
            }
            createContext("/api/job-fit-report") { exchange ->
                val requestBody = exchange.requestBody.bufferedReader().use { it.readText() }
                val root = mapper.readTree(requestBody)
                val jobTitle = root.path("jobTitle").asText("Target Role")
                val payload = mapper.writeValueAsBytes(
                    mapOf(
                        "headline" to "Local embedding structured report",
                        "fitBand" to "MEDIUM",
                        "summary" to "Structured summary for $jobTitle",
                        "strengths" to listOf("Strong semantic overlap"),
                        "risks" to listOf("One or more missing skills remain"),
                        "improvementSuggestions" to listOf("Add stronger project evidence for the missing tools"),
                        "nextSteps" to listOf("Tune resume keywords toward the job description"),
                        "narrative" to "岗位适应性报告与技能提升建议：你与「$jobTitle」已有较强对齐，但仍需继续补强缺失能力。",
                    ),
                )
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(200, payload.size.toLong())
                exchange.responseBody.use { output: OutputStream -> output.write(payload) }
            }
            start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
            registry.add("spring.flyway.enabled") { true }
            registry.add("spring.flyway.locations") { "classpath:db/migration" }
            registry.add("app.resume-queue.listener-enabled") { false }
            registry.add("app.embedding.ai-service-base-url") { "http://127.0.0.1:18083" }
            registry.add("app.embedding.embedding-path") { "/api/embeddings" }
            registry.add("app.embedding.job-fit-report-path") { "/api/job-fit-report" }
            registry.add("app.embedding.native-vector-storage-enabled") { false }
            registry.add("app.embedding.pgvector-query-enabled") { false }
        }

        private fun buildVector(text: String): List<Double> {
            val values = DoubleArray(1536)
            val tokens = text.lowercase().split(Regex("[^a-z0-9+#.]+"))
                .filter { it.isNotBlank() }
                .ifEmpty { listOf("smartats") }

            tokens.forEachIndexed { tokenIndex, token ->
                token.forEachIndexed { charIndex, char ->
                    val position = (tokenIndex * 31 + charIndex) % values.size
                    values[position] += (char.code % 97) / 97.0
                }
            }

            val norm = sqrt(values.sumOf { it * it }).takeIf { it > 0.0 } ?: 1.0
            return values.map { it / norm }
        }
    }
}