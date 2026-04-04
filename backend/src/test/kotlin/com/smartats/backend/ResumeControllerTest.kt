package com.smartats.backend

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.smartats.backend.domain.Resume
import com.smartats.backend.queue.ResumeParseMessage
import com.smartats.backend.queue.ResumeQueueProducer
import com.smartats.backend.repository.ResumeRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mockingDetails
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResumeControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var resumeRepository: ResumeRepository

    @MockBean
    private lateinit var resumeQueueProducer: ResumeQueueProducer

    @BeforeEach
    fun setUp() {
        resumeRepository.deleteAll()
    }

    @Test
    fun `create resume stores parsed data and defaults status to pending parse`() {
        val requestBody = mapOf(
            "candidateName" to "Jane Doe",
            "contactInfo" to "jane@example.com",
            "rawContentReference" to "/tmp/resumes/jane-doe.pdf",
            "parsedData" to mapOf(
                "education" to listOf("BSc Computer Science"),
                "skills" to listOf("Kotlin", "SQL"),
                "radar" to mapOf("communication" to 4.5, "problemSolving" to 4.8),
            ),
        )

        val createResult = mockMvc.perform(
            post("/api/resumes")
                .with(user("resume_admin").roles("HR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.status").value("PENDING_PARSE"))
            .andExpect(jsonPath("$.data.parsedData.education[0]").value("BSc Computer Science"))
            .andExpect(jsonPath("$.data.parsedData.radar.communication").value(4.5))
            .andReturn()

        val resumeId = extractId(createResult.response.contentAsString)

        mockMvc.perform(
            get("/api/resumes/{resumeId}", resumeId)
                .with(user("resume_admin").roles("HR")),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.id").value(resumeId))
            .andExpect(jsonPath("$.data.parsedData.skills[1]").value("SQL"))
    }

    @Test
    fun `resume endpoint round trips json payload when authenticated`() {
        val requestBody = mapOf(
            "candidateName" to "Resume User",
            "contactInfo" to "resume@example.com",
            "rawContentReference" to "/tmp/resumes/resume-user.pdf",
            "parsedData" to mapOf(
                "projects" to listOf(mapOf("name" to "ATS", "impact" to "high")),
                "score" to 92,
            ),
        )

        val createResult = mockMvc.perform(
            post("/api/resumes")
                .with(user("resume_admin").roles("HR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.status").value("PENDING_PARSE"))
            .andExpect(jsonPath("$.data.parsedData.projects[0].name").value("ATS"))
            .andReturn()

        val resumeId = extractId(createResult.response.contentAsString)

        mockMvc.perform(
            get("/api/resumes/{resumeId}", resumeId)
                .with(user("resume_admin").roles("HR")),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.id").value(resumeId))
            .andExpect(jsonPath("$.data.parsedData.score").value(92))
            .andExpect(jsonPath("$.data.rawContentReference").value("/tmp/resumes/resume-user.pdf"))
    }

    @Test
    fun `status endpoint returns current parsing progress`() {
        val resume = resumeRepository.save(
            Resume(
                candidateName = "Status Candidate",
                contactInfo = "status@example.com",
                rawContentReference = "/tmp/resumes/status-candidate.pdf",
                parsedData = null,
                status = "PARSING",
            ),
        )

        mockMvc.perform(
            get("/api/resumes/{resumeId}/status", requireNotNull(resume.id))
                .with(user("resume_admin").roles("HR")),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.resumeId").value(requireNotNull(resume.id).toString()))
            .andExpect(jsonPath("$.data.status").value("PARSING"))
            .andExpect(jsonPath("$.data.parsedDataAvailable").value(false))
    }

    @Test
    fun `parse endpoint publishes resume parse message`() {
        val resume = resumeRepository.save(
            Resume(
                candidateName = "Queue Candidate",
                contactInfo = "queue@example.com",
                rawContentReference = "/tmp/resumes/queue-candidate.pdf",
                parsedData = null,
                status = "PENDING_PARSE",
            ),
        )

        val resumeId = requireNotNull(resume.id)

        mockMvc.perform(
            post("/api/resumes/{resumeId}/parse", resumeId)
                .with(user("resume_admin").roles("HR")),
        )
            .andExpect(status().isAccepted)
            .andExpect(jsonPath("$.data.resumeId").value(resumeId.toString()))
            .andExpect(jsonPath("$.data.queued").value(true))
            .andExpect(jsonPath("$.data.status").value("PENDING_PARSE"))
            .andExpect(jsonPath("$.message").value("Resume parse queued"))

        val invocation = mockingDetails(resumeQueueProducer).invocations.single {
            it.method.name == "publish"
        }
        val publishedMessage = invocation.arguments.first() as ResumeParseMessage
        assertEquals(resumeId, publishedMessage.resumeId)
        assertEquals("/tmp/resumes/queue-candidate.pdf", publishedMessage.rawContentReference)
    }

    @Test
    fun `internal callback stores parsed result and marks resume parsed`() {
        val resume = resumeRepository.save(
            Resume(
                candidateName = "Callback Candidate",
                contactInfo = "callback@example.com",
                rawContentReference = "/tmp/resumes/callback-candidate.pdf",
                parsedData = null,
                status = "PARSING",
            ),
        )

        val resumeId = requireNotNull(resume.id)
        val requestBody = mapOf(
            "parsedData" to mapOf(
                "candidateProfile" to mapOf(
                    "name" to "Callback Candidate",
                    "skills" to listOf("Kotlin", "Spring Boot"),
                ),
                "xaiSummary" to "Mock structured result",
            ),
        )

        mockMvc.perform(
            post("/internal/api/resumes/{resumeId}/parsed-result", resumeId)
                .header("X-Internal-Api-Key", "test-internal-callback-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.resumeId").value(resumeId.toString()))
            .andExpect(jsonPath("$.data.status").value("PARSED"))
            .andExpect(jsonPath("$.data.parsedDataAvailable").value(true))

        mockMvc.perform(
            get("/api/resumes/{resumeId}/status", resumeId)
                .with(user("resume_admin").roles("HR")),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("PARSED"))
            .andExpect(jsonPath("$.data.parsedDataAvailable").value(true))

        mockMvc.perform(
            get("/api/resumes/{resumeId}", resumeId)
                .with(user("resume_admin").roles("HR")),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.parsedData.candidateProfile.skills[0]").value("Kotlin"))
            .andExpect(jsonPath("$.data.status").value("PARSED"))
    }

    @Test
    fun `internal callback rejects malformed json`() {
        val resume = resumeRepository.save(
            Resume(
                candidateName = "Malformed Candidate",
                contactInfo = "malformed@example.com",
                rawContentReference = "/tmp/resumes/malformed.pdf",
                parsedData = null,
                status = "PARSING",
            ),
        )

        mockMvc.perform(
            post("/internal/api/resumes/{resumeId}/parsed-result", requireNotNull(resume.id))
                .header("X-Internal-Api-Key", "test-internal-callback-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"parsedData\":"),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `internal callback returns not found for unknown resume id`() {
        val unknownResumeId = java.util.UUID.randomUUID()
        val requestBody = mapOf(
            "parsedData" to mapOf("candidateProfile" to mapOf("name" to "Ghost Candidate")),
        )

        mockMvc.perform(
            post("/internal/api/resumes/{resumeId}/parsed-result", unknownResumeId)
                .header("X-Internal-Api-Key", "test-internal-callback-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Resume not found"))
    }

    @Test
    fun `internal callback rejects invalid api key`() {
        val resume = resumeRepository.save(
            Resume(
                candidateName = "Protected Candidate",
                contactInfo = "protected@example.com",
                rawContentReference = "/tmp/resumes/protected.pdf",
                parsedData = null,
                status = "PARSING",
            ),
        )

        val requestBody = mapOf(
            "parsedData" to mapOf("candidateProfile" to mapOf("name" to "Protected Candidate")),
        )

        mockMvc.perform(
            post("/internal/api/resumes/{resumeId}/parsed-result", requireNotNull(resume.id))
                .header("X-Internal-Api-Key", "wrong-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("Invalid internal API key"))
    }

    @Test
    fun `internal parse failed callback marks resume as parse failed with reason`() {
        val resume = resumeRepository.save(
            Resume(
                candidateName = "Failure Candidate",
                contactInfo = "failure@example.com",
                rawContentReference = "/tmp/resumes/failure.pdf",
                parsedData = mapOf("stale" to true),
                parseFailureReason = null,
                status = "PARSING",
            ),
        )

        val resumeId = requireNotNull(resume.id)
        val requestBody = mapOf(
            "reason" to "Vision model failed to parse uploaded image",
        )

        mockMvc.perform(
            post("/internal/api/resumes/{resumeId}/parse-failed", resumeId)
                .header("X-Internal-Api-Key", "test-internal-callback-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.resumeId").value(resumeId.toString()))
            .andExpect(jsonPath("$.data.status").value("PARSE_FAILED"))
            .andExpect(jsonPath("$.data.parsedDataAvailable").value(false))
            .andExpect(jsonPath("$.data.parseFailureReason").value("Vision model failed to parse uploaded image"))

        mockMvc.perform(
            get("/api/resumes/{resumeId}/status", resumeId)
                .with(user("resume_admin").roles("HR")),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("PARSE_FAILED"))
            .andExpect(jsonPath("$.data.parseFailureReason").value("Vision model failed to parse uploaded image"))

        mockMvc.perform(
            get("/api/resumes/{resumeId}", resumeId)
                .with(user("resume_admin").roles("HR")),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("PARSE_FAILED"))
            .andExpect(jsonPath("$.data.parseFailureReason").value("Vision model failed to parse uploaded image"))
            .andExpect(jsonPath("$.data.parsedData").doesNotExist())
    }

    @Test
    fun `internal parse failed callback rejects already parsed resume`() {
        val resume = resumeRepository.save(
            Resume(
                candidateName = "Parsed Candidate",
                contactInfo = "parsed@example.com",
                rawContentReference = "/tmp/resumes/parsed.pdf",
                parsedData = mapOf("candidateProfile" to mapOf("name" to "Parsed Candidate")),
                parseFailureReason = null,
                status = "PARSED",
            ),
        )

        mockMvc.perform(
            post("/internal/api/resumes/{resumeId}/parse-failed", requireNotNull(resume.id))
                .header("X-Internal-Api-Key", "test-internal-callback-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("reason" to "Late AI timeout"))),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Cannot mark a parsed resume as failed"))
    }

    private fun extractId(json: String): String {
        val root: JsonNode = objectMapper.readTree(json)
        return root.path("data").path("id").asText()
    }
}