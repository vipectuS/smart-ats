package com.smartats.backend

import com.fasterxml.jackson.databind.ObjectMapper
import com.smartats.backend.domain.Job
import com.smartats.backend.domain.JobApplication
import com.smartats.backend.domain.JobApplicationStatus
import com.smartats.backend.domain.JobFavorite
import com.smartats.backend.domain.JobFavoriteStatus
import com.smartats.backend.domain.JobIgnore
import com.smartats.backend.domain.JobIgnoreStatus
import com.smartats.backend.domain.Resume
import com.smartats.backend.domain.User
import com.smartats.backend.domain.UserRole
import com.smartats.backend.dto.auth.LoginRequest
import com.smartats.backend.repository.JobApplicationRepository
import com.smartats.backend.repository.JobFavoriteRepository
import com.smartats.backend.repository.JobIgnoreRepository
import com.smartats.backend.repository.JobRecommendationRepository
import com.smartats.backend.repository.JobRepository
import com.smartats.backend.repository.ResumeRepository
import com.smartats.backend.repository.UserRepository
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CandidateControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var resumeRepository: ResumeRepository

    @Autowired
    private lateinit var jobRepository: JobRepository

    @Autowired
    private lateinit var jobRecommendationRepository: JobRecommendationRepository

    @Autowired
    private lateinit var jobApplicationRepository: JobApplicationRepository

    @Autowired
    private lateinit var jobFavoriteRepository: JobFavoriteRepository

    @Autowired
    private lateinit var jobIgnoreRepository: JobIgnoreRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun setUp() {
        jobRecommendationRepository.deleteAll()
        jobApplicationRepository.deleteAll()
        jobFavoriteRepository.deleteAll()
        jobIgnoreRepository.deleteAll()
        jobRepository.deleteAll()
        resumeRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `candidate can update profile and view latest parsed resume data`() {
        val accessToken = obtainAccessToken("candidate_profile", "candidate_profile@example.com", UserRole.CANDIDATE)
        val candidate = userRepository.findByUsername("candidate_profile").orElseThrow()

        resumeRepository.save(
            Resume(
                candidateName = "Candidate Profile",
                contactInfo = "candidate_profile@example.com",
                rawContentReference = "s3://resumes/candidate-profile.pdf",
                parsedData = sampleParsedData(
                    fullName = "Candidate Profile",
                    email = "candidate_profile@example.com",
                    skills = listOf("Kotlin", "Docker"),
                    summary = "Built backend systems and CI pipelines",
                ),
                ownerUser = candidate,
                status = "PARSED",
            ),
        )

        mockMvc.perform(
            put("/api/candidate/profile")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "githubUrl" to "https://github.com/candidate_profile",
                            "portfolioUrl" to "https://candidate.dev/portfolio",
                        ),
                    ),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.githubUrl").value("https://github.com/candidate_profile"))
            .andExpect(jsonPath("$.data.portfolioUrl").value("https://candidate.dev/portfolio"))

        mockMvc.perform(
            get("/api/candidate/profile")
                .header("Authorization", "Bearer $accessToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.username").value("candidate_profile"))
            .andExpect(jsonPath("$.data.latestResume.status").value("PARSED"))
            .andExpect(jsonPath("$.data.latestResume.parsedData.basicInfo.fullName").value("Candidate Profile"))
            .andExpect(jsonPath("$.data.latestResume.parsedData.skills[0].name").value("Kotlin"))
    }

    @Test
    fun `candidate upload binds resume to authenticated user`() {
        val accessToken = obtainAccessToken("resume_owner", "resume_owner@example.com", UserRole.CANDIDATE)
        val owner = userRepository.findByUsername("resume_owner").orElseThrow()

        mockMvc.perform(
            post("/api/resumes/upload")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "rawContentReference" to "s3://resumes/resume-owner.pdf",
                        ),
                    ),
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.message").value("Resume uploaded"))
            .andExpect(jsonPath("$.data.candidateName").value("resume_owner"))
            .andExpect(jsonPath("$.data.contactInfo").value("resume_owner@example.com"))

        val savedResume = resumeRepository.findAll().single()
        assertNotNull(savedResume.ownerUser)
        assertEquals(owner.id, savedResume.ownerUser?.id)
    }

    @Test
    fun `candidate reverse matching returns candidate-facing suitability report`() {
        val candidateToken = obtainAccessToken("market_candidate", "market_candidate@example.com", UserRole.CANDIDATE)
        val candidate = userRepository.findByUsername("market_candidate").orElseThrow()
        val hrOwner = ensureUser("hr_owner", "hr_owner@example.com", UserRole.HR)

        resumeRepository.save(
            Resume(
                candidateName = "Market Candidate",
                contactInfo = "market_candidate@example.com",
                rawContentReference = "s3://resumes/market-candidate.pdf",
                parsedData = sampleParsedData(
                    fullName = "Market Candidate",
                    email = "market_candidate@example.com",
                    skills = listOf("Java", "Spring Boot", "Kotlin"),
                    summary = "Built Java services and backend APIs for marketplace systems",
                ),
                ownerUser = candidate,
                status = "PARSED",
            ),
        )

        jobRepository.save(
            Job(
                title = "Java Platform Engineer",
                description = "Build Java Spring Boot services with Docker and PostgreSQL",
                requirements = mapOf(
                    "skills" to listOf("Java", "Spring Boot", "Docker", "PostgreSQL"),
                    "experienceYears" to 3,
                ),
                createdBy = hrOwner,
            ),
        )
        jobRepository.save(
            Job(
                title = "Frontend Designer",
                description = "Need Figma, CSS and animation skills",
                requirements = mapOf("skills" to listOf("Figma", "CSS")),
                createdBy = hrOwner,
            ),
        )

        mockMvc.perform(
            post("/api/candidate/match-jobs")
                .header("Authorization", "Bearer $candidateToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.evaluatedCount").value(2))
            .andExpect(jsonPath("$.data.recommendations[0].title").value("Java Platform Engineer"))
            .andExpect(jsonPath("$.data.recommendations[0].suitabilityReport").value(containsString("岗位适应性报告与技能提升建议")))
            .andExpect(jsonPath("$.data.recommendations[0].suitabilityReport").value(containsString("Docker")))
            .andExpect(jsonPath("$.data.recommendations[0].xaiReport.fitBand").value("MEDIUM"))
            .andExpect(jsonPath("$.data.recommendations[0].xaiReport.improvementSuggestions[0]").exists())
            .andExpect(jsonPath("$.data.recommendations[0].actionState.applied").value(false))
                .andExpect(jsonPath("$.data.recommendations[0].matchedSkills[0]").value("Java"))
    }

    @Test
    fun `candidate action endpoints are idempotent and list records`() {
        val candidateToken = obtainAccessToken("action_candidate", "action_candidate@example.com", UserRole.CANDIDATE)
        val hrOwner = ensureUser("action_hr", "action_hr@example.com", UserRole.HR)
        val applyJob = jobRepository.save(
            Job(
                title = "Applied Role",
                description = "Backend role for action flow",
                requirements = mapOf("skills" to listOf("Kotlin")),
                createdBy = hrOwner,
            ),
        )
        val favoriteJob = jobRepository.save(
            Job(
                title = "Favorited Role",
                description = "Platform role for action flow",
                requirements = mapOf("skills" to listOf("PostgreSQL")),
                createdBy = hrOwner,
            ),
        )
        val ignoredJob = jobRepository.save(
            Job(
                title = "Ignored Role",
                description = "Frontend role for action flow",
                requirements = mapOf("skills" to listOf("CSS")),
                createdBy = hrOwner,
            ),
        )

        mockMvc.perform(
            post("/api/jobs/{jobId}/apply", requireNotNull(applyJob.id))
                .header("Authorization", "Bearer $candidateToken"),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.created").value(true))
            .andExpect(jsonPath("$.data.changed").value(true))
            .andExpect(jsonPath("$.data.active").value(true))
            .andExpect(jsonPath("$.data.actionState.applied").value(true))

        mockMvc.perform(
            post("/api/jobs/{jobId}/apply", requireNotNull(applyJob.id))
                .header("Authorization", "Bearer $candidateToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.created").value(false))
            .andExpect(jsonPath("$.data.changed").value(false))
            .andExpect(jsonPath("$.data.active").value(true))

        mockMvc.perform(
            post("/api/jobs/{jobId}/favorite", requireNotNull(favoriteJob.id))
                .header("Authorization", "Bearer $candidateToken"),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.active").value(true))
            .andExpect(jsonPath("$.data.actionState.favorited").value(true))

        mockMvc.perform(
            post("/api/jobs/{jobId}/ignore", requireNotNull(ignoredJob.id))
                .header("Authorization", "Bearer $candidateToken"),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.active").value(true))
            .andExpect(jsonPath("$.data.actionState.ignored").value(true))

        mockMvc.perform(
            delete("/api/jobs/{jobId}/apply", requireNotNull(applyJob.id))
                .header("Authorization", "Bearer $candidateToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.changed").value(true))
            .andExpect(jsonPath("$.data.active").value(false))
            .andExpect(jsonPath("$.data.actionState.applied").value(false))

        mockMvc.perform(
            delete("/api/jobs/{jobId}/apply", requireNotNull(applyJob.id))
                .header("Authorization", "Bearer $candidateToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.changed").value(false))
            .andExpect(jsonPath("$.data.active").value(false))

        mockMvc.perform(
            post("/api/jobs/{jobId}/apply", requireNotNull(applyJob.id))
                .header("Authorization", "Bearer $candidateToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.created").value(false))
            .andExpect(jsonPath("$.data.changed").value(true))
            .andExpect(jsonPath("$.data.active").value(true))
            .andExpect(jsonPath("$.data.actionState.applied").value(true))

        mockMvc.perform(
            delete("/api/jobs/{jobId}/favorite", requireNotNull(favoriteJob.id))
                .header("Authorization", "Bearer $candidateToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.changed").value(true))
            .andExpect(jsonPath("$.data.active").value(false))
            .andExpect(jsonPath("$.data.actionState.favorited").value(false))

        mockMvc.perform(
            delete("/api/jobs/{jobId}/favorite", requireNotNull(favoriteJob.id))
                .header("Authorization", "Bearer $candidateToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.changed").value(false))
            .andExpect(jsonPath("$.data.active").value(false))

        mockMvc.perform(
            delete("/api/jobs/{jobId}/ignore", requireNotNull(ignoredJob.id))
                .header("Authorization", "Bearer $candidateToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.changed").value(true))
            .andExpect(jsonPath("$.data.active").value(false))
            .andExpect(jsonPath("$.data.actionState.ignored").value(false))

        mockMvc.perform(
            delete("/api/jobs/{jobId}/ignore", requireNotNull(ignoredJob.id))
                .header("Authorization", "Bearer $candidateToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.changed").value(false))
            .andExpect(jsonPath("$.data.active").value(false))

        mockMvc.perform(
            delete("/api/jobs/{jobId}/favorite", requireNotNull(applyJob.id))
                .header("Authorization", "Bearer $candidateToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.changed").value(false))

        mockMvc.perform(
            delete("/api/jobs/{jobId}/ignore", requireNotNull(favoriteJob.id))
                .header("Authorization", "Bearer $candidateToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.changed").value(false))

        mockMvc.perform(
            get("/api/candidate/applications")
                .header("Authorization", "Bearer $candidateToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].title").value("Applied Role"))
            .andExpect(jsonPath("$.data[0].actionType").value("APPLIED"))

        mockMvc.perform(
            get("/api/candidate/favorites")
                .header("Authorization", "Bearer $candidateToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(0))

        mockMvc.perform(
            get("/api/candidate/ignores")
                .header("Authorization", "Bearer $candidateToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(0))
    }

    @Test
    fun `ignored jobs are excluded from reverse matching until unignored`() {
        val candidateToken = obtainAccessToken("ignore_candidate", "ignore_candidate@example.com", UserRole.CANDIDATE)
        val candidate = userRepository.findByUsername("ignore_candidate").orElseThrow()
        val hrOwner = ensureUser("ignore_hr", "ignore_hr@example.com", UserRole.HR)

        resumeRepository.save(
            Resume(
                candidateName = "Ignore Candidate",
                contactInfo = "ignore_candidate@example.com",
                rawContentReference = "s3://resumes/ignore-candidate.pdf",
                parsedData = sampleParsedData(
                    fullName = "Ignore Candidate",
                    email = "ignore_candidate@example.com",
                    skills = listOf("Java", "Spring Boot", "Docker"),
                    summary = "Strong backend platform experience",
                ),
                ownerUser = candidate,
                status = "PARSED",
            ),
        )

        val javaJob = jobRepository.save(
            Job(
                title = "Java Platform Engineer",
                description = "Build Java Spring Boot services with Docker",
                requirements = mapOf("skills" to listOf("Java", "Spring Boot", "Docker")),
                createdBy = hrOwner,
            ),
        )
        jobRepository.save(
            Job(
                title = "Frontend Designer",
                description = "Need Figma and CSS",
                requirements = mapOf("skills" to listOf("Figma", "CSS")),
                createdBy = hrOwner,
            ),
        )

        mockMvc.perform(
            post("/api/jobs/{jobId}/ignore", requireNotNull(javaJob.id))
                .header("Authorization", "Bearer $candidateToken"),
        )
            .andExpect(status().isCreated)

        mockMvc.perform(
            post("/api/candidate/match-jobs")
                .header("Authorization", "Bearer $candidateToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.evaluatedCount").value(1))
            .andExpect(jsonPath("$.data.recommendations[0].title").value("Frontend Designer"))

        mockMvc.perform(
            delete("/api/jobs/{jobId}/ignore", requireNotNull(javaJob.id))
                .header("Authorization", "Bearer $candidateToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.changed").value(true))

        mockMvc.perform(
            post("/api/candidate/match-jobs")
                .header("Authorization", "Bearer $candidateToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.evaluatedCount").value(2))
            .andExpect(jsonPath("$.data.recommendations[0].title").value("Java Platform Engineer"))
    }

    @Test
    fun `candidate timeline aggregates application favorite and ignore records in descending timestamp order`() {
        val candidateToken = obtainAccessToken("timeline_candidate", "timeline_candidate@example.com", UserRole.CANDIDATE)
        val candidate = userRepository.findByUsername("timeline_candidate").orElseThrow()
        val hrOwner = ensureUser("timeline_hr", "timeline_hr@example.com", UserRole.HR)

        val appliedJob = jobRepository.save(
            Job(
                title = "Applied Role",
                description = "Backend timeline role",
                requirements = mapOf("skills" to listOf("Kotlin")),
                createdBy = hrOwner,
            ),
        )
        val favoriteJob = jobRepository.save(
            Job(
                title = "Favorite Role",
                description = "Platform timeline role",
                requirements = mapOf("skills" to listOf("PostgreSQL")),
                createdBy = hrOwner,
            ),
        )
        val ignoredJob = jobRepository.save(
            Job(
                title = "Ignored Role",
                description = "Frontend timeline role",
                requirements = mapOf("skills" to listOf("CSS")),
                createdBy = hrOwner,
            ),
        )

        val application = jobApplicationRepository.save(
            JobApplication(
                user = candidate,
                job = appliedJob,
                status = JobApplicationStatus.WITHDRAWN,
                createdAt = LocalDateTime.of(2026, 4, 4, 8, 0, 0),
                updatedAt = LocalDateTime.of(2026, 4, 4, 9, 30, 0),
            ),
        )
        val favorite = jobFavoriteRepository.save(
            JobFavorite(
                user = candidate,
                job = favoriteJob,
                status = JobFavoriteStatus.FAVORITED,
                createdAt = LocalDateTime.of(2026, 4, 4, 8, 30, 0),
                updatedAt = LocalDateTime.of(2026, 4, 4, 10, 30, 0),
            ),
        )
        val ignore = jobIgnoreRepository.save(
            JobIgnore(
                user = candidate,
                job = ignoredJob,
                status = JobIgnoreStatus.UNIGNORED,
                createdAt = LocalDateTime.of(2026, 4, 4, 7, 0, 0),
                updatedAt = LocalDateTime.of(2026, 4, 4, 8, 45, 0),
            ),
        )

        jdbcTemplate.update(
            "UPDATE job_applications SET created_at = ?, updated_at = ? WHERE id = ?",
            LocalDateTime.of(2026, 4, 4, 8, 0, 0),
            LocalDateTime.of(2026, 4, 4, 9, 30, 0),
            requireNotNull(application.id),
        )
        jdbcTemplate.update(
            "UPDATE job_favorites SET created_at = ?, updated_at = ? WHERE id = ?",
            LocalDateTime.of(2026, 4, 4, 8, 30, 0),
            LocalDateTime.of(2026, 4, 4, 10, 30, 0),
            requireNotNull(favorite.id),
        )
        jdbcTemplate.update(
            "UPDATE job_ignores SET created_at = ?, updated_at = ? WHERE id = ?",
            LocalDateTime.of(2026, 4, 4, 7, 0, 0),
            LocalDateTime.of(2026, 4, 4, 8, 45, 0),
            requireNotNull(ignore.id),
        )

        mockMvc.perform(
            get("/api/candidates/me/timeline")
                .header("Authorization", "Bearer $candidateToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(3))
            .andExpect(jsonPath("$.data[0].action").value("FAVORITED"))
            .andExpect(jsonPath("$.data[0].jobTitle").value("Favorite Role"))
            .andExpect(jsonPath("$.data[0].companyName").value("timeline_hr"))
            .andExpect(jsonPath("$.data[0].timestamp").value("2026-04-04T10:30:00Z"))
            .andExpect(jsonPath("$.data[1].action").value("WITHDRAWN"))
            .andExpect(jsonPath("$.data[1].jobTitle").value("Applied Role"))
            .andExpect(jsonPath("$.data[2].action").value("UNIGNORED"))
            .andExpect(jsonPath("$.data[2].jobTitle").value("Ignored Role"))
    }

    @Test
    fun `hr user cannot call candidate matching endpoints`() {
        val hrToken = obtainAccessToken("hr_only", "hr_only@example.com", UserRole.HR)

        mockMvc.perform(
            post("/api/candidate/match-jobs")
                .header("Authorization", "Bearer $hrToken"),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `candidate cannot access hr dashboard stats`() {
        val candidateToken = obtainAccessToken("dashboard_candidate", "dashboard_candidate@example.com", UserRole.CANDIDATE)

        mockMvc.perform(
            get("/api/hr/dashboard/stats")
                .header("Authorization", "Bearer $candidateToken"),
        )
            .andExpect(status().isForbidden)
    }

    private fun obtainAccessToken(username: String, email: String, role: UserRole): String {
        ensureUser(username, email, role)

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

    private fun ensureUser(username: String, email: String, role: UserRole): User {
        return userRepository.findByUsername(username).orElseGet {
            userRepository.save(
                User(
                    username = username,
                    passwordHash = passwordEncoder.encode("Password123"),
                    email = email,
                    role = role,
                ),
            )
        }
    }

    private fun sampleParsedData(
        fullName: String,
        email: String,
        skills: List<String>,
        summary: String,
    ): Map<String, Any> {
        return mapOf(
            "basicInfo" to mapOf(
                "fullName" to fullName,
                "email" to email,
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
                    "endDate" to "2025-01",
                    "responsibilities" to listOf("Build backend services", "Optimize pipelines"),
                    "achievements" to listOf("Improved matching quality"),
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
                "technicalDepth" to 8,
                "problemSolving" to 9,
                "collaboration" to 8,
                "leadership" to 7,
                "adaptability" to 8,
            ),
            "xaiReasoning" to "Structured candidate profile",
        )
    }
}