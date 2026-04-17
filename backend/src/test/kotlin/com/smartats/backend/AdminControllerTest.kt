package com.smartats.backend

import com.fasterxml.jackson.databind.ObjectMapper
import com.smartats.backend.domain.Job
import com.smartats.backend.domain.Resume
import com.smartats.backend.domain.SkillDictionaryEntry
import com.smartats.backend.domain.User
import com.smartats.backend.domain.UserRole
import com.smartats.backend.dto.auth.LoginRequest
import com.smartats.backend.repository.JobRepository
import com.smartats.backend.repository.ResumeRepository
import com.smartats.backend.repository.SkillDictionaryRepository
import com.smartats.backend.repository.UserRepository
import com.smartats.backend.service.ResumeService
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
class AdminControllerTest {

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
    private lateinit var skillDictionaryRepository: SkillDictionaryRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @BeforeEach
    fun setUp() {
        resumeRepository.deleteAll()
        jobRepository.deleteAll()
        skillDictionaryRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `admin can view overview and parse failures`() {
        val adminToken = obtainAccessToken("admin_ops", "admin_ops@example.com", UserRole.ADMIN)
        val candidate = userRepository.save(
            User(
                username = "broken_candidate",
                passwordHash = passwordEncoder.encode("Password123"),
                email = "broken_candidate@example.com",
                role = UserRole.CANDIDATE,
            ),
        )

        jobRepository.save(
            Job(
                title = "Platform Engineer",
                description = "Maintain ATS delivery stack",
            ),
        )
        skillDictionaryRepository.save(
            SkillDictionaryEntry(
                name = "Kotlin",
                category = "backend",
                aliases = listOf("Spring Kotlin"),
            ),
        )
        resumeRepository.save(
            Resume(
                candidateName = "Broken Candidate",
                contactInfo = candidate.email,
                rawContentReference = "s3://resumes/C99_broken.pdf",
                browserPreprocessedPayload = mapOf("sourceFileName" to "C99_broken.pdf"),
                ownerUser = candidate,
                status = ResumeService.STATUS_PARSE_FAILED,
                parseFailureReason = "Mock parser could not infer timeline",
            ),
        )
        resumeRepository.save(
            Resume(
                candidateName = "Healthy Candidate",
                contactInfo = "healthy@example.com",
                rawContentReference = "s3://resumes/C01_healthy.pdf",
                status = ResumeService.STATUS_PARSED,
                parsedData = mapOf("basicInfo" to mapOf("fullName" to "Healthy Candidate")),
            ),
        )

        mockMvc.perform(
            get("/api/admin/overview")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.totals.totalUsers").value(2))
            .andExpect(jsonPath("$.data.totals.totalJobs").value(1))
            .andExpect(jsonPath("$.data.totals.totalResumes").value(2))
            .andExpect(jsonPath("$.data.totals.totalSkillEntries").value(1))
            .andExpect(jsonPath("$.data.usersByRole[2].label").value("ADMIN"))
            .andExpect(jsonPath("$.data.usersByRole[2].value").value(1))
            .andExpect(jsonPath("$.data.resumesByStatus[3].label").value("PARSE_FAILED"))
            .andExpect(jsonPath("$.data.resumesByStatus[3].value").value(1))
            .andExpect(jsonPath("$.data.latestParseFailures[0].ownerUsername").value("broken_candidate"))
            .andExpect(jsonPath("$.data.latestParseFailures[0].sourceFileName").value("C99_broken.pdf"))

        mockMvc.perform(
            get("/api/admin/parse-failures")
                .header("Authorization", "Bearer $adminToken")
                .param("limit", "10"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].reason").value("Mock parser could not infer timeline"))
    }

    @Test
    fun `admin can create and update skill dictionary entries`() {
        val adminToken = obtainAccessToken("admin_skill", "admin_skill@example.com", UserRole.ADMIN)

        val createResponse = mockMvc.perform(
            post("/api/admin/skills")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "name" to " TypeScript ",
                            "category" to "frontend",
                            "aliases" to listOf("TS", "typescript", ""),
                            "enabled" to true,
                        ),
                    ),
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.name").value("TypeScript"))
            .andExpect(jsonPath("$.data.aliases.length()").value(1))
            .andReturn()

        val skillId = objectMapper.readTree(createResponse.response.contentAsString)
            .path("data")
            .path("id")
            .asText()

        mockMvc.perform(
            put("/api/admin/skills/{skillId}", skillId)
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "name" to "TypeScript",
                            "category" to "engineering",
                            "aliases" to listOf("TS", "Node TS"),
                            "enabled" to false,
                        ),
                    ),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.category").value("engineering"))
            .andExpect(jsonPath("$.data.enabled").value(false))
            .andExpect(jsonPath("$.data.aliases.length()").value(2))

        mockMvc.perform(
            get("/api/admin/skills")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].name").value("TypeScript"))
    }

    @Test
    fun `non admin user cannot access admin endpoints`() {
        val hrToken = obtainAccessToken("hr_viewer", "hr_viewer@example.com", UserRole.HR)

        mockMvc.perform(
            get("/api/admin/overview")
                .header("Authorization", "Bearer $hrToken"),
        )
            .andExpect(status().isForbidden)
    }

    private fun obtainAccessToken(username: String, email: String, role: UserRole): String {
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
}