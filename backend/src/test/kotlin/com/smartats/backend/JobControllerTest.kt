package com.smartats.backend

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.smartats.backend.domain.Job
import com.smartats.backend.domain.Resume
import com.smartats.backend.domain.User
import com.smartats.backend.domain.UserRole
import com.smartats.backend.repository.JobApplicationRepository
import com.smartats.backend.dto.auth.LoginRequest
import com.smartats.backend.repository.JobRecommendationRepository
import com.smartats.backend.repository.JobRepository
import com.smartats.backend.repository.ResumeRepository
import com.smartats.backend.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JobControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var jobRepository: JobRepository

    @Autowired
    private lateinit var resumeRepository: ResumeRepository

    @Autowired
    private lateinit var jobRecommendationRepository: JobRecommendationRepository

    @Autowired
    private lateinit var jobApplicationRepository: JobApplicationRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @BeforeEach
    fun setUp() {
        jobRecommendationRepository.deleteAll()
        jobRepository.deleteAll()
        resumeRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `create job persists jsonb requirements and returns detail`() {
        val accessToken = obtainAccessToken("job_owner", "job_owner@example.com")
        val requestBody = mapOf(
            "title" to "Senior Kotlin Engineer",
            "description" to "Build secure ATS services",
            "requirements" to mapOf(
                "skills" to listOf("Kotlin", "Spring Boot", "PostgreSQL"),
                "experienceYears" to 5,
                "remote" to true,
            ),
        )

        val createResult = mockMvc.perform(
            post("/api/jobs")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.title").value("Senior Kotlin Engineer"))
            .andExpect(jsonPath("$.data.requirements.skills[1]").value("Spring Boot"))
            .andExpect(jsonPath("$.data.createdBy.username").value("job_owner"))
            .andReturn()

        val jobId = extractId(createResult.response.contentAsString)

        mockMvc.perform(
            get("/api/jobs/{jobId}", jobId)
                .header("Authorization", "Bearer $accessToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.id").value(jobId))
            .andExpect(jsonPath("$.data.requirements.experienceYears").value(5))
            .andExpect(jsonPath("$.data.createdBy.role").value("HR"))
    }

    @Test
    fun `list jobs returns paginated results`() {
        val accessToken = obtainAccessToken("page_user", "page_user@example.com")

        repeat(3) { index ->
            val requestBody = mapOf(
                "title" to "Backend Engineer ${index + 1}",
                "description" to "Maintain hiring workflow ${index + 1}",
                "requirements" to mapOf("priority" to index + 1, "skills" to listOf("Kotlin")),
            )

            mockMvc.perform(
                post("/api/jobs")
                    .header("Authorization", "Bearer $accessToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody)),
            )
                .andExpect(status().isCreated)
        }

        mockMvc.perform(
            get("/api/jobs")
                .header("Authorization", "Bearer $accessToken")
                .param("page", "0")
                .param("size", "2"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content.length()").value(2))
            .andExpect(jsonPath("$.data.page").value(0))
            .andExpect(jsonPath("$.data.size").value(2))
            .andExpect(jsonPath("$.data.totalElements").value(3))
            .andExpect(jsonPath("$.data.totalPages").value(2))
    }

    @Test
    fun `update job persists modified requirements and fields`() {
        val accessToken = obtainAccessToken("editor_hr", "editor_hr@example.com")

        val createResult = mockMvc.perform(
            post("/api/jobs")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "title" to "Frontend Engineer",
                            "description" to "Build dashboard interfaces",
                            "requirements" to mapOf(
                                "skills" to listOf("Vue", "TypeScript"),
                                "location" to "Shanghai",
                            ),
                        ),
                    ),
                ),
        )
            .andExpect(status().isCreated)
            .andReturn()

        val jobId = extractId(createResult.response.contentAsString)

        mockMvc.perform(
            put("/api/jobs/{jobId}", jobId)
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "title" to "Senior Frontend Engineer",
                            "description" to "Lead dashboard and workflow experience improvements",
                            "requirements" to mapOf(
                                "skills" to listOf("Vue", "TypeScript", "ECharts"),
                                "location" to "Hangzhou",
                                "headcount" to 2,
                            ),
                        ),
                    ),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Job updated"))
            .andExpect(jsonPath("$.data.title").value("Senior Frontend Engineer"))
            .andExpect(jsonPath("$.data.requirements.skills[2]").value("ECharts"))
            .andExpect(jsonPath("$.data.requirements.location").value("Hangzhou"))
            .andExpect(jsonPath("$.data.requirements.headcount").value(2))

        mockMvc.perform(
            get("/api/jobs/{jobId}", jobId)
                .header("Authorization", "Bearer $accessToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.title").value("Senior Frontend Engineer"))
            .andExpect(jsonPath("$.data.description").value("Lead dashboard and workflow experience improvements"))
    }

    @Test
    fun `update job rejects hr who is not creator`() {
        val ownerToken = obtainAccessToken("job_author", "job_author@example.com")
        val otherHrToken = obtainAccessToken("other_hr", "other_hr@example.com")

        val createResult = mockMvc.perform(
            post("/api/jobs")
                .header("Authorization", "Bearer $ownerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "title" to "Platform Engineer",
                            "description" to "Maintain ATS platform",
                            "requirements" to mapOf("skills" to listOf("Kotlin", "PostgreSQL")),
                        ),
                    ),
                ),
        )
            .andExpect(status().isCreated)
            .andReturn()

        val jobId = extractId(createResult.response.contentAsString)

        mockMvc.perform(
            put("/api/jobs/{jobId}", jobId)
                .header("Authorization", "Bearer $otherHrToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "title" to "Unauthorized Update",
                            "description" to "Should not succeed",
                            "requirements" to mapOf("skills" to listOf("Forbidden")),
                        ),
                    ),
                ),
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("Only the job creator or an admin can update this job"))
    }

    @Test
    fun `list job applications returns active applicants with latest resume summary`() {
        val hrToken = obtainAccessToken("review_hr", "review_hr@example.com")
        val candidateToken = obtainAccessToken("active_candidate", "active_candidate@example.com", UserRole.CANDIDATE)
        val candidate = userRepository.findByUsername("active_candidate").orElseThrow()
        val owner = userRepository.findByUsername("review_hr").orElseThrow()

        val job = jobRepository.save(
            Job(
                title = "Applied Role",
                description = "Review live job applications",
                requirements = mapOf("skills" to listOf("Kotlin", "Vue")),
                createdBy = owner,
            ),
        )

        val resume = resumeRepository.save(
            Resume(
                candidateName = "Active Candidate",
                contactInfo = "active_candidate@example.com",
                rawContentReference = "s3://resumes/active-candidate.pdf",
                parsedData = mapOf(
                    "basicInfo" to mapOf(
                        "fullName" to "Active Candidate",
                        "email" to "active_candidate@example.com",
                    ),
                ),
                ownerUser = candidate,
                status = "PARSED",
            ),
        )

        mockMvc.perform(
            post("/api/jobs/{jobId}/apply", job.id)
                .header("Authorization", "Bearer $candidateToken"),
        )
            .andExpect(status().isCreated)

        mockMvc.perform(
            get("/api/jobs/{jobId}/applications", job.id)
                .header("Authorization", "Bearer $hrToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].status").value("APPLIED"))
            .andExpect(jsonPath("$.data[0].candidate.username").value("active_candidate"))
            .andExpect(jsonPath("$.data[0].candidate.displayName").value("Active Candidate"))
            .andExpect(jsonPath("$.data[0].latestResume.resumeId").value(requireNotNull(resume.id).toString()))
            .andExpect(jsonPath("$.data[0].latestResume.status").value("PARSED"))
    }

    @Test
    fun `list job applications rejects hr who is not creator`() {
        val ownerToken = obtainAccessToken("creator_hr", "creator_hr@example.com")
        val otherHrToken = obtainAccessToken("viewer_hr", "viewer_hr@example.com")

        val createResult = mockMvc.perform(
            post("/api/jobs")
                .header("Authorization", "Bearer $ownerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "title" to "Restricted Job",
                            "description" to "Only creator can review applications",
                            "requirements" to mapOf("skills" to listOf("Security")),
                        ),
                    ),
                ),
        )
            .andExpect(status().isCreated)
            .andReturn()

        val jobId = extractId(createResult.response.contentAsString)

        mockMvc.perform(
            get("/api/jobs/{jobId}/applications", jobId)
                .header("Authorization", "Bearer $otherHrToken"),
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("Only the job creator or an admin can review applications for this job"))
    }

    @Test
    fun `review job application updates interview status and note`() {
        val hrToken = obtainAccessToken("workflow_hr", "workflow_hr@example.com")
        val candidateToken = obtainAccessToken("workflow_candidate", "workflow_candidate@example.com", UserRole.CANDIDATE)
        val candidate = userRepository.findByUsername("workflow_candidate").orElseThrow()
        val owner = userRepository.findByUsername("workflow_hr").orElseThrow()

        val job = jobRepository.save(
            Job(
                title = "Workflow Role",
                description = "HR review workflow role",
                requirements = mapOf("skills" to listOf("Kotlin")),
                createdBy = owner,
            ),
        )

        resumeRepository.save(
            Resume(
                candidateName = "Workflow Candidate",
                contactInfo = "workflow_candidate@example.com",
                rawContentReference = "s3://resumes/workflow-candidate.pdf",
                ownerUser = candidate,
                status = "PARSED",
            ),
        )

        mockMvc.perform(
            post("/api/jobs/{jobId}/apply", job.id)
                .header("Authorization", "Bearer $candidateToken"),
        )
            .andExpect(status().isCreated)

        val application = jobApplicationRepository.findByUserIdAndJobId(requireNotNull(candidate.id), requireNotNull(job.id))
            ?: throw IllegalStateException("Application should exist after apply")

        mockMvc.perform(
            put("/api/jobs/{jobId}/applications/{applicationId}", job.id, requireNotNull(application.id))
                .header("Authorization", "Bearer $hrToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "status" to "INTERVIEW",
                            "reviewNote" to "已安排下周技术面，重点追问分布式缓存经验。",
                        ),
                    ),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Application review updated"))
            .andExpect(jsonPath("$.data.status").value("INTERVIEW"))
            .andExpect(jsonPath("$.data.reviewNote").value("已安排下周技术面，重点追问分布式缓存经验。"))

        mockMvc.perform(
            get("/api/jobs/{jobId}/applications", job.id)
                .header("Authorization", "Bearer $hrToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].status").value("INTERVIEW"))
            .andExpect(jsonPath("$.data[0].reviewNote").value("已安排下周技术面，重点追问分布式缓存经验。"))
    }

    @Test
    fun `evaluate job generates recommendations and persists them`() {
        val accessToken = obtainAccessToken("matcher", "matcher@example.com")
        val owner = userRepository.findByUsername("matcher").orElseThrow()

        val job = jobRepository.save(
            Job(
                title = "Senior Kotlin Engineer",
                description = "Build secure ATS services with Kotlin Spring Boot PostgreSQL and Redis",
                requirements = mapOf(
                    "skills" to listOf("Kotlin", "Spring Boot", "PostgreSQL", "Redis"),
                    "level" to "senior",
                ),
                createdBy = owner,
            ),
        )

        createParsedResume(
            fullName = "Alice Chen",
            email = "alice@example.com",
            skills = listOf("Kotlin", "Spring Boot", "PostgreSQL", "Redis"),
            summary = "Built secure ATS platforms and backend services",
            radarBase = 9,
        )
        createParsedResume(
            fullName = "Bob Li",
            email = "bob@example.com",
            skills = listOf("Java", "Spring Boot"),
            summary = "Worked on internal workflow tools",
            radarBase = 7,
        )
        resumeRepository.save(
            Resume(
                candidateName = "Pending Candidate",
                contactInfo = "pending@example.com",
                rawContentReference = "s3://resumes/pending.pdf",
                status = "PENDING_PARSE",
            ),
        )

        mockMvc.perform(
            post("/api/jobs/{jobId}/evaluate", job.id)
                .header("Authorization", "Bearer $accessToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Recommendations generated"))
            .andExpect(jsonPath("$.data.jobId").value(job.id.toString()))
            .andExpect(jsonPath("$.data.evaluatedCount").value(2))
            .andExpect(jsonPath("$.data.recommendations.length()").value(2))
            .andExpect(jsonPath("$.data.recommendations[0].candidate.basicInfo.fullName").value("Alice Chen"))
            .andExpect(jsonPath("$.data.recommendations[0].candidate.radarScores.technicalDepth").value(9))
            .andExpect(jsonPath("$.data.recommendations[0].xaiReasoning").value(org.hamcrest.Matchers.containsString("Hybrid score")))
            .andExpect(jsonPath("$.data.recommendations[0].xaiReport.headline").exists())
            .andExpect(jsonPath("$.data.recommendations[0].matchScore").isNumber)

        val persisted = jobRecommendationRepository.findByJobId(requireNotNull(job.id))
        org.junit.jupiter.api.Assertions.assertEquals(2, persisted.size)
    }

    @Test
    fun `evaluate job can be repeated without recommendation uniqueness conflicts`() {
        val accessToken = obtainAccessToken("repeat_eval", "repeat_eval@example.com")
        val owner = userRepository.findByUsername("repeat_eval").orElseThrow()

        val job = jobRepository.save(
            Job(
                title = "Repeatable Evaluation Engineer",
                description = "Re-run recommendation batches safely",
                requirements = mapOf("skills" to listOf("Kotlin", "Redis")),
                createdBy = owner,
            ),
        )

        createParsedResume(
            fullName = "Repeat Candidate",
            email = "repeat_candidate@example.com",
            skills = listOf("Kotlin", "Redis"),
            summary = "Re-evaluates hiring batches cleanly",
            radarBase = 8,
        )

        mockMvc.perform(
            post("/api/jobs/{jobId}/evaluate", job.id)
                .header("Authorization", "Bearer $accessToken"),
        )
            .andExpect(status().isOk)

        mockMvc.perform(
            post("/api/jobs/{jobId}/evaluate", job.id)
                .header("Authorization", "Bearer $accessToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.evaluatedCount").value(1))

        val persisted = jobRecommendationRepository.findByJobId(requireNotNull(job.id))
        org.junit.jupiter.api.Assertions.assertEquals(1, persisted.size)
    }

    @Test
    fun `evaluate job accepts custom hr weights and reflects them in xai`() {
        val accessToken = obtainAccessToken("weighted_hr", "weighted_hr@example.com")
        val owner = userRepository.findByUsername("weighted_hr").orElseThrow()

        val job = jobRepository.save(
            Job(
                title = "Semantic Search Engineer",
                description = "Need vector retrieval, Python, ranking and semantic search experience",
                requirements = mapOf(
                    "skills" to listOf("Python", "Ranking", "Vector"),
                    "experienceYears" to 3,
                    "educationKeywords" to listOf("computer", "science"),
                ),
                createdBy = owner,
            ),
        )

        createParsedResume(
            fullName = "Diana Zhang",
            email = "diana@example.com",
            skills = listOf("Python", "Vector", "Ranking"),
            summary = "Built semantic retrieval and ranking systems for search applications",
            radarBase = 9,
        )

        mockMvc.perform(
            post("/api/jobs/{jobId}/evaluate", job.id)
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "skillWeight" to 20,
                            "experienceWeight" to 10,
                            "educationWeight" to 10,
                            "semanticWeight" to 60,
                        ),
                    ),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.appliedWeights.skillWeight").value(20.00))
            .andExpect(jsonPath("$.data.appliedWeights.semanticWeight").value(60.00))
            .andExpect(jsonPath("$.data.recommendations[0].xaiReasoning").value(org.hamcrest.Matchers.containsString("skills 20.00%")))
            .andExpect(jsonPath("$.data.recommendations[0].xaiReasoning").value(org.hamcrest.Matchers.containsString("semantic 60.00%")))
    }

    @Test
    fun `evaluate job rejects all-zero weights`() {
        val accessToken = obtainAccessToken("invalid_weight_hr", "invalid_weight_hr@example.com")
        val owner = userRepository.findByUsername("invalid_weight_hr").orElseThrow()

        val job = jobRepository.save(
            Job(
                title = "Backend Engineer",
                description = "Build APIs",
                requirements = mapOf("skills" to listOf("Kotlin")),
                createdBy = owner,
            ),
        )

        mockMvc.perform(
            post("/api/jobs/{jobId}/evaluate", job.id)
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "skillWeight" to 0,
                            "experienceWeight" to 0,
                            "educationWeight" to 0,
                            "semanticWeight" to 0,
                        ),
                    ),
                ),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `list recommendations returns enriched candidate fields`() {
        val accessToken = obtainAccessToken("reviewer", "reviewer@example.com")
        val owner = userRepository.findByUsername("reviewer").orElseThrow()

        val job = jobRepository.save(
            Job(
                title = "Platform Engineer",
                description = "Need Kotlin PostgreSQL system design skills",
                requirements = mapOf("skills" to listOf("Kotlin", "PostgreSQL")),
                createdBy = owner,
            ),
        )

        createParsedResume(
            fullName = "Carol Wu",
            email = "carol@example.com",
            skills = listOf("Kotlin", "PostgreSQL", "Docker"),
            summary = "Designs resilient backend systems",
            radarBase = 8,
        )

        mockMvc.perform(
            post("/api/jobs/{jobId}/evaluate", job.id)
                .header("Authorization", "Bearer $accessToken"),
        )
            .andExpect(status().isOk)

        mockMvc.perform(
            get("/api/jobs/{jobId}/recommendations", job.id)
                .header("Authorization", "Bearer $accessToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].candidate.basicInfo.fullName").value("Carol Wu"))
            .andExpect(jsonPath("$.data[0].candidate.skills[0].name").value("Kotlin"))
            .andExpect(jsonPath("$.data[0].xaiReport.summary").exists())
            .andExpect(jsonPath("$.data[0].candidate.parsedData.basicInfo.email").value("carol@example.com"))
            .andExpect(jsonPath("$.data[0].candidate.radarScores.problemSolving").value(8))
    }

    private fun obtainAccessToken(username: String, email: String, role: UserRole = UserRole.HR): String {
        userRepository.save(
            User(
                username = username,
                passwordHash = passwordEncoder.encode("Password123"),
                email = email,
                role = role,
            ),
        )

        val loginResponse = mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(LoginRequest(username = username, password = "Password123"))),
        )
            .andExpect(status().isOk)
            .andReturn()

        return objectMapper.readTree(loginResponse.response.contentAsString)
            .path("data")
            .path("accessToken")
            .asText()
    }

    private fun extractId(json: String): String {
        val root: JsonNode = objectMapper.readTree(json)
        return root.path("data").path("id").asText()
    }

    private fun createParsedResume(
        fullName: String,
        email: String,
        skills: List<String>,
        summary: String,
        radarBase: Int,
    ): Resume {
        return resumeRepository.save(
            Resume(
                candidateName = fullName,
                contactInfo = email,
                rawContentReference = "s3://resumes/${fullName.lowercase().replace(" ", "-")}.pdf",
                parsedData = mapOf(
                    "basicInfo" to mapOf(
                        "fullName" to fullName,
                        "email" to email,
                        "phone" to "13800000000",
                        "location" to "Shanghai",
                        "headline" to "Backend Engineer",
                        "summary" to summary,
                    ),
                    "workExperiences" to listOf(
                        mapOf(
                            "company" to "Smart ATS",
                            "title" to "Backend Engineer",
                            "startDate" to "2022-01",
                            "endDate" to "2025-01",
                            "responsibilities" to listOf("Build Kotlin services", "Design PostgreSQL schema"),
                            "achievements" to listOf("Improved ATS pipeline", "Launched scoring engine"),
                        ),
                    ),
                    "educationExperiences" to listOf(
                        mapOf(
                            "school" to "Fudan University",
                            "degree" to "Bachelor",
                            "fieldOfStudy" to "Computer Science",
                            "startDate" to "2016-09",
                            "endDate" to "2020-06",
                        ),
                    ),
                    "skills" to skills.map {
                        mapOf(
                            "name" to it,
                            "category" to "technical",
                            "proficiency" to "advanced",
                            "evidence" to "Used in production services",
                        )
                    },
                    "radarScores" to mapOf(
                        "communication" to radarBase,
                        "technicalDepth" to radarBase,
                        "problemSolving" to radarBase,
                        "collaboration" to radarBase,
                        "leadership" to radarBase - 1,
                        "adaptability" to radarBase,
                    ),
                    "xaiReasoning" to "Candidate profile extracted successfully",
                ),
                status = "PARSED",
            ),
        )
    }
}