package com.smartats.backend.repository

import com.smartats.backend.domain.Job
import com.smartats.backend.domain.JobRecommendation
import com.smartats.backend.domain.Resume
import com.smartats.backend.domain.User
import com.smartats.backend.domain.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal

@SpringBootTest
@ActiveProfiles("test")
class JobRecommendationRepositoryTest {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var jobRepository: JobRepository

    @Autowired
    private lateinit var resumeRepository: ResumeRepository

    @Autowired
    private lateinit var jobRecommendationRepository: JobRecommendationRepository

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
    fun `job recommendation persists recommendation record`() {
        val creator = userRepository.save(
            User(
                username = "recommendation_owner",
                passwordHash = passwordEncoder.encode("Password123"),
                email = "recommendation_owner@example.com",
                role = UserRole.HR,
            ),
        )

        val job = jobRepository.save(
            Job(
                title = "Principal Platform Engineer",
                description = "Own the hiring platform backbone",
                requirements = mapOf("skills" to listOf("Kotlin", "PostgreSQL")),
                createdBy = creator,
            ),
        )

        val resume = resumeRepository.save(
            Resume(
                candidateName = "Recommendation Candidate",
                contactInfo = "candidate@example.com",
                rawContentReference = "/tmp/resumes/recommendation.pdf",
                parsedData = mapOf(
                    "basicInfo" to mapOf("fullName" to "Recommendation Candidate"),
                    "skills" to listOf(mapOf("name" to "Kotlin")),
                    "radarScores" to mapOf(
                        "communication" to 80,
                        "technicalDepth" to 90,
                        "problemSolving" to 88,
                        "collaboration" to 85,
                        "leadership" to 78,
                        "adaptability" to 86,
                    ),
                    "xaiReasoning" to "Promising profile",
                ),
                parseFailureReason = null,
                status = "PARSED",
            ),
        )

        val savedRecommendation = jobRecommendationRepository.save(
            JobRecommendation(
                job = job,
                resume = resume,
                matchScore = BigDecimal("87.50"),
                xaiReasoning = "High technical alignment and solid communication score",
                xaiReport = mapOf(
                    "headline" to "Strong fit",
                    "fitBand" to "HIGH",
                    "summary" to "Candidate aligns closely with the role",
                    "strengths" to listOf("Kotlin"),
                    "risks" to listOf("None"),
                    "improvementSuggestions" to listOf("Keep sharpening PostgreSQL depth"),
                    "nextSteps" to listOf("Prepare interview stories"),
                    "narrative" to "Structured report narrative",
                ),
            ),
        )

        val byJob = jobRecommendationRepository.findByJobId(requireNotNull(job.id))
        val byResume = jobRecommendationRepository.findByResumeId(requireNotNull(resume.id))

        assertEquals(requireNotNull(savedRecommendation.id), requireNotNull(byJob.single().id))
        assertEquals(BigDecimal("87.50"), byResume.single().matchScore)
        assertEquals("High technical alignment and solid communication score", byResume.single().xaiReasoning)
        assertEquals("HIGH", byResume.single().xaiReport?.get("fitBand"))
    }
}