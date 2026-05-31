package com.smartats.backend

import com.fasterxml.jackson.databind.ObjectMapper
import com.smartats.backend.domain.AccessAuditEvent
import com.smartats.backend.domain.AdminParseFailureReviewStatus
import com.smartats.backend.domain.AccessAuditActionType
import com.smartats.backend.domain.AccessAuditActorRole
import com.smartats.backend.domain.AccessAuditSensitiveField
import com.smartats.backend.domain.AccessAuditTargetType
import com.smartats.backend.domain.Job
import com.smartats.backend.domain.Organization
import com.smartats.backend.domain.Resume
import com.smartats.backend.domain.SkillDictionaryEntry
import com.smartats.backend.domain.User
import com.smartats.backend.domain.UserRole
import com.smartats.backend.dto.auth.LoginRequest
import com.smartats.backend.queue.ResumeParseMessage
import com.smartats.backend.queue.ResumeQueueProducer
import com.smartats.backend.repository.AccessAuditEventRepository
import com.smartats.backend.repository.AdminParseFailureReviewEventRepository
import com.smartats.backend.repository.JobRepository
import com.smartats.backend.repository.OrganizationRepository
import com.smartats.backend.repository.ResumeRepository
import com.smartats.backend.repository.SkillDictionaryRepository
import com.smartats.backend.repository.UserRepository
import com.smartats.backend.service.AccessAuditService
import com.smartats.backend.service.ResumeService
import com.smartats.backend.service.OrganizationService
import org.hamcrest.Matchers.contains
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mockingDetails
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
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
    private lateinit var organizationRepository: OrganizationRepository

    @Autowired
    private lateinit var resumeRepository: ResumeRepository

    @Autowired
    private lateinit var skillDictionaryRepository: SkillDictionaryRepository

    @Autowired
    private lateinit var adminParseFailureReviewEventRepository: AdminParseFailureReviewEventRepository

    @Autowired
    private lateinit var accessAuditEventRepository: AccessAuditEventRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @MockBean
    private lateinit var resumeQueueProducer: ResumeQueueProducer

    @BeforeEach
    fun setUp() {
        accessAuditEventRepository.deleteAll()
        adminParseFailureReviewEventRepository.deleteAll()
        resumeRepository.deleteAll()
        jobRepository.deleteAll()
        skillDictionaryRepository.deleteAllInBatch()
        userRepository.deleteAll()
    }

    @Test
    fun `admin can view review and retry parse failures`() {
        val adminToken = obtainAccessToken("admin_ops", "admin_ops@example.com", UserRole.ADMIN)
        val candidate = userRepository.save(
            User(
                username = "broken_candidate",
                passwordHash = passwordEncoder.encode("Password123"),
                email = "broken_candidate@example.com",
                role = UserRole.CANDIDATE,
            ),
        )
        val organization = organizationRepository.findAll().firstOrNull()
            ?: organizationRepository.save(
                Organization(
                    name = "Test Org",
                    tokenHash = passwordEncoder.encode("org-token"),
                    tokenPreview = "org_****",
                    enabled = true,
                ),
            )

        jobRepository.save(
            Job(
                title = "Platform Engineer",
                description = "Maintain ATS delivery stack",
                organization = organization,
            ),
        )
        skillDictionaryRepository.save(
            SkillDictionaryEntry(
                name = "Kotlin",
                category = "backend",
                aliases = listOf("Spring Kotlin"),
            ),
        )
        val failedResume = resumeRepository.save(
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
            .andExpect(jsonPath("$.data.latestParseFailures[0].reviewStatus").value("UNREVIEWED"))

        mockMvc.perform(
            get("/api/admin/access-audit-events")
                .header("Authorization", "Bearer $adminToken")
                .param("limit", "10")
                .param("actionType", AccessAuditActionType.ADMIN_OVERVIEW_VIEWED.name),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].actorUsername").value("admin_ops"))
            .andExpect(jsonPath("$.data[0].actorRole").value("ADMIN"))
            .andExpect(jsonPath("$.data[0].actionType").value("ADMIN_OVERVIEW_VIEWED"))
            .andExpect(jsonPath("$.data[0].targetType").value("SYSTEM_OVERVIEW"))
            .andExpect(jsonPath("$.data[0].targetId").value(AccessAuditService.SYSTEM_OVERVIEW_TARGET_ID.toString()))

        mockMvc.perform(
            put("/api/admin/parse-failures/{resumeId}/review", failedResume.id)
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "note" to "人工确认时间线字段缺失，建议候选人补充后再试",
                            "reviewStatus" to "NEEDS_CANDIDATE_UPDATE",
                        ),
                    ),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.adminReviewNote").value("人工确认时间线字段缺失，建议候选人补充后再试"))
            .andExpect(jsonPath("$.data.reviewStatus").value("NEEDS_CANDIDATE_UPDATE"))
            .andExpect(jsonPath("$.data.reviewedByUsername").value("admin_ops"))

        mockMvc.perform(
            get("/api/admin/parse-failures")
                .header("Authorization", "Bearer $adminToken")
                .param("limit", "10"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].reason").value("Mock parser could not infer timeline"))
            .andExpect(jsonPath("$.data[0].adminReviewNote").value("人工确认时间线字段缺失，建议候选人补充后再试"))
            .andExpect(jsonPath("$.data[0].reviewStatus").value("NEEDS_CANDIDATE_UPDATE"))

        val parseFailuresViewedAfterFirstList = accessAuditEventRepository.findAll()
            .filter { it.actionType == AccessAuditActionType.PARSE_FAILURES_VIEWED }
        assertEquals(1, parseFailuresViewedAfterFirstList.size)
        assertEquals(failedResume.id, parseFailuresViewedAfterFirstList.single().targetId)

        val reviewUpdatedEvent = accessAuditEventRepository.findAll().single {
            it.actionType == AccessAuditActionType.PARSE_FAILURE_REVIEW_UPDATED
        }
        assertEquals("admin_ops", reviewUpdatedEvent.actorUsername)
        assertEquals(AccessAuditActorRole.ADMIN, reviewUpdatedEvent.actorRole)
        assertEquals(AccessAuditTargetType.RESUME, reviewUpdatedEvent.targetType)
        assertEquals(failedResume.id, reviewUpdatedEvent.targetId)

        mockMvc.perform(
            get("/api/admin/parse-failures")
                .header("Authorization", "Bearer $adminToken")
                .param("limit", "10")
                .param("reviewStatus", AdminParseFailureReviewStatus.NEEDS_CANDIDATE_UPDATE.name),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].resumeId").value(failedResume.id.toString()))

        val parseFailuresViewedAfterFilteredList = accessAuditEventRepository.findAll()
            .filter { it.actionType == AccessAuditActionType.PARSE_FAILURES_VIEWED }
        assertEquals(2, parseFailuresViewedAfterFilteredList.size)

        mockMvc.perform(
            post("/api/admin/parse-failures/{resumeId}/retry", failedResume.id)
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("note" to "管理员复核后重新入队"))),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.resumeId").value(failedResume.id.toString()))
            .andExpect(jsonPath("$.data.status").value("PENDING_PARSE"))
            .andExpect(jsonPath("$.data.queued").value(true))

        val retryQueuedEvent = accessAuditEventRepository.findAll().single {
            it.actionType == AccessAuditActionType.PARSE_FAILURE_RETRY_QUEUED
        }
        assertEquals("admin_ops", retryQueuedEvent.actorUsername)
        assertEquals(AccessAuditActorRole.ADMIN, retryQueuedEvent.actorRole)
        assertEquals(AccessAuditTargetType.RESUME, retryQueuedEvent.targetType)
        assertEquals(failedResume.id, retryQueuedEvent.targetId)

        mockMvc.perform(
            get("/api/admin/parse-failures")
                .header("Authorization", "Bearer $adminToken")
                .param("limit", "10"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(0))

        val retriedResume = resumeRepository.findById(failedResume.id!!).orElseThrow()
        assertEquals(ResumeService.STATUS_PENDING_PARSE, retriedResume.status)
        assertEquals("管理员复核后重新入队", retriedResume.adminReviewNote)
        assertEquals("admin_ops", retriedResume.adminReviewedBy)
        assertEquals(AdminParseFailureReviewStatus.APPROVED_FOR_RETRY, retriedResume.adminReviewStatus)

        mockMvc.perform(
            get("/api/admin/parse-failures/{resumeId}/review-events", failedResume.id)
                .header("Authorization", "Bearer $adminToken")
                .param("limit", "10"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].actionType").value("RETRY_QUEUED"))
            .andExpect(jsonPath("$.data[0].adminUsername").value("admin_ops"))
            .andExpect(jsonPath("$.data[0].note").value("管理员复核后重新入队"))
            .andExpect(jsonPath("$.data[0].previousReviewStatus").value("NEEDS_CANDIDATE_UPDATE"))
            .andExpect(jsonPath("$.data[0].nextReviewStatus").value("APPROVED_FOR_RETRY"))
            .andExpect(jsonPath("$.data[0].resumeStatusAfterAction").value("PENDING_PARSE"))
            .andExpect(jsonPath("$.data[1].actionType").value("REVIEW_SAVED"))
            .andExpect(jsonPath("$.data[1].previousReviewStatus").value("UNREVIEWED"))
            .andExpect(jsonPath("$.data[1].nextReviewStatus").value("NEEDS_CANDIDATE_UPDATE"))
            .andExpect(jsonPath("$.data[1].resumeStatusAfterAction").value("PARSE_FAILED"))

        val reviewEventsViewedAuditEvent = accessAuditEventRepository.findAll().single {
            it.actionType == AccessAuditActionType.PARSE_FAILURE_REVIEW_EVENTS_VIEWED
        }
        assertEquals("admin_ops", reviewEventsViewedAuditEvent.actorUsername)
        assertEquals(AccessAuditActorRole.ADMIN, reviewEventsViewedAuditEvent.actorRole)
        assertEquals(AccessAuditTargetType.RESUME, reviewEventsViewedAuditEvent.targetType)
        assertEquals(failedResume.id, reviewEventsViewedAuditEvent.targetId)

        val exportResponse = mockMvc.perform(
            get("/api/admin/parse-failures/{resumeId}/review-events/export", failedResume.id)
                .header("Authorization", "Bearer $adminToken")
                .param("limit", "10"),
        )
            .andExpect(status().isOk)
            .andReturn()

        assertTrue(exportResponse.response.contentType?.startsWith("text/csv") == true)
        assertTrue(exportResponse.response.getHeader("Content-Disposition")!!.contains("parse-failure-review-events-${failedResume.id}.csv"))
        assertTrue(exportResponse.response.contentAsString.contains("actionType"))
        assertTrue(exportResponse.response.contentAsString.contains("RETRY_QUEUED"))
        assertTrue(exportResponse.response.contentAsString.contains("REVIEW_SAVED"))

        mockMvc.perform(
            get("/api/admin/access-audit-events")
                .header("Authorization", "Bearer $adminToken")
                .param("limit", "10")
                .param("actionType", AccessAuditActionType.PARSE_FAILURE_REVIEW_EVENTS_EXPORTED.name),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].actorUsername").value("admin_ops"))
            .andExpect(jsonPath("$.data[0].actorRole").value("ADMIN"))
            .andExpect(jsonPath("$.data[0].actionType").value("PARSE_FAILURE_REVIEW_EVENTS_EXPORTED"))
            .andExpect(jsonPath("$.data[0].targetType").value("RESUME"))
            .andExpect(jsonPath("$.data[0].targetId").value(failedResume.id.toString()))

        val publishedMessage = mockingDetails(resumeQueueProducer).invocations.single {
            it.method.name == "publish"
        }.arguments[0] as ResumeParseMessage
        assertEquals(failedResume.id, publishedMessage.resumeId)
    }

    @Test
    fun `admin can batch review and retry parse failures`() {
        val adminToken = obtainAccessToken("admin_batch", "admin_batch@example.com", UserRole.ADMIN)
        val candidate = userRepository.save(
            User(
                username = "batch_candidate",
                passwordHash = passwordEncoder.encode("Password123"),
                email = "batch_candidate@example.com",
                role = UserRole.CANDIDATE,
            ),
        )

        val firstFailedResume = resumeRepository.save(
            Resume(
                candidateName = "Batch Candidate 1",
                contactInfo = candidate.email,
                rawContentReference = "s3://resumes/C201_batch_1.pdf",
                browserPreprocessedPayload = mapOf("sourceFileName" to "C201_batch_1.pdf"),
                ownerUser = candidate,
                status = ResumeService.STATUS_PARSE_FAILED,
                parseFailureReason = "Mock parser could not infer employment dates",
            ),
        )
        val secondFailedResume = resumeRepository.save(
            Resume(
                candidateName = "Batch Candidate 2",
                contactInfo = candidate.email,
                rawContentReference = "s3://resumes/C202_batch_2.pdf",
                browserPreprocessedPayload = mapOf("sourceFileName" to "C202_batch_2.pdf"),
                ownerUser = candidate,
                status = ResumeService.STATUS_PARSE_FAILED,
                parseFailureReason = "Mock parser returned low-confidence OCR content",
            ),
        )

        val resumeIds = listOf(requireNotNull(firstFailedResume.id), requireNotNull(secondFailedResume.id))

        mockMvc.perform(
            put("/api/admin/parse-failures/review")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "resumeIds" to resumeIds,
                            "note" to "批量确认：先补材料，再决定是否重试",
                            "reviewStatus" to "NEEDS_CANDIDATE_UPDATE",
                        ),
                    ),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.processedCount").value(2))
            .andExpect(jsonPath("$.data.reviewStatus").value("NEEDS_CANDIDATE_UPDATE"))
            .andExpect(jsonPath("$.data.queued").value(false))

        mockMvc.perform(
            post("/api/admin/parse-failures/retry")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "resumeIds" to resumeIds,
                            "note" to "批量复核通过，统一重新入队",
                        ),
                    ),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.processedCount").value(2))
            .andExpect(jsonPath("$.data.reviewStatus").value("APPROVED_FOR_RETRY"))
            .andExpect(jsonPath("$.data.queued").value(true))

        val refreshedResumes = resumeRepository.findAllById(resumeIds).associateBy { requireNotNull(it.id) }
        resumeIds.forEach { resumeId ->
            val refreshedResume = refreshedResumes.getValue(resumeId)
            assertEquals(ResumeService.STATUS_PENDING_PARSE, refreshedResume.status)
            assertEquals(AdminParseFailureReviewStatus.APPROVED_FOR_RETRY, refreshedResume.adminReviewStatus)
            assertEquals("admin_batch", refreshedResume.adminReviewedBy)
            assertEquals("批量复核通过，统一重新入队", refreshedResume.adminReviewNote)
        }

        val reviewAuditEvents = accessAuditEventRepository.findAll().filter {
            it.actionType == AccessAuditActionType.PARSE_FAILURE_REVIEW_UPDATED
        }
        assertEquals(2, reviewAuditEvents.size)
        val retryAuditEvents = accessAuditEventRepository.findAll().filter {
            it.actionType == AccessAuditActionType.PARSE_FAILURE_RETRY_QUEUED
        }
        assertEquals(2, retryAuditEvents.size)

        val publishedMessages = mockingDetails(resumeQueueProducer).invocations
            .filter { it.method.name == "publish" }
            .map { it.arguments[0] as ResumeParseMessage }
        assertEquals(2, publishedMessages.size)
        assertEquals(resumeIds.toSet(), publishedMessages.map { it.resumeId }.toSet())
    }

    @Test
    fun `admin can view and export parse failure summary`() {
        val adminToken = obtainAccessToken("admin_summary", "admin_summary@example.com", UserRole.ADMIN)
        val candidate = userRepository.save(
            User(
                username = "summary_candidate",
                passwordHash = passwordEncoder.encode("Password123"),
                email = "summary_candidate@example.com",
                role = UserRole.CANDIDATE,
            ),
        )

        resumeRepository.saveAll(
            listOf(
                Resume(
                    candidateName = "Summary Candidate 1",
                    contactInfo = candidate.email,
                    rawContentReference = "s3://resumes/summary-1.pdf",
                    ownerUser = candidate,
                    status = ResumeService.STATUS_PARSE_FAILED,
                    parseFailureReason = "[OCR_LOW_CONFIDENCE] Mock parser returned low-confidence OCR content",
                    adminReviewStatus = AdminParseFailureReviewStatus.UNREVIEWED,
                ),
                Resume(
                    candidateName = "Summary Candidate 2",
                    contactInfo = candidate.email,
                    rawContentReference = "s3://resumes/summary-2.pdf",
                    ownerUser = candidate,
                    status = ResumeService.STATUS_PARSE_FAILED,
                    parseFailureReason = "[RESUME_PARSE_TIMEOUT] Callback timeout while waiting for upstream model",
                    adminReviewStatus = AdminParseFailureReviewStatus.NEEDS_CANDIDATE_UPDATE,
                ),
                Resume(
                    candidateName = "Summary Candidate 3",
                    contactInfo = candidate.email,
                    rawContentReference = "s3://resumes/summary-3.pdf",
                    ownerUser = candidate,
                    status = ResumeService.STATUS_PARSE_FAILED,
                    parseFailureReason = "Unclassified parser failure reason",
                    adminReviewStatus = AdminParseFailureReviewStatus.APPROVED_FOR_RETRY,
                ),
            ),
        )

        mockMvc.perform(
            get("/api/admin/parse-failures/summary")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.totalFailures").value(3))
            .andExpect(jsonPath("$.data.firstFailureAt").isNotEmpty)
            .andExpect(jsonPath("$.data.lastFailureAt").isNotEmpty)
            .andExpect(jsonPath("$.data.reviewStatusCounts[?(@.label=='UNREVIEWED')].value").value(contains(1)))
            .andExpect(jsonPath("$.data.reviewStatusCounts[?(@.label=='NEEDS_CANDIDATE_UPDATE')].value").value(contains(1)))
            .andExpect(jsonPath("$.data.reviewStatusCounts[?(@.label=='APPROVED_FOR_RETRY')].value").value(contains(1)))
            .andExpect(jsonPath("$.data.failureCodeCounts[?(@.label=='OCR_LOW_CONFIDENCE')].value").value(contains(1)))
            .andExpect(jsonPath("$.data.failureCodeCounts[?(@.label=='RESUME_PARSE_TIMEOUT')].value").value(contains(1)))
            .andExpect(jsonPath("$.data.failureCodeCounts[?(@.label=='UNCLASSIFIED')].value").value(contains(1)))

        val exportResponse = mockMvc.perform(
            get("/api/admin/parse-failures/summary/export")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andReturn()

        assertTrue(exportResponse.response.contentType?.startsWith("text/csv") == true)
        assertTrue(exportResponse.response.getHeader("Content-Disposition")!!.contains("parse-failure-summary.csv"))
        assertTrue(exportResponse.response.contentAsString.contains("meta,totalFailures,3"))
        assertTrue(exportResponse.response.contentAsString.contains("reviewStatus,UNREVIEWED,1"))
        assertTrue(exportResponse.response.contentAsString.contains("failureCode,OCR_LOW_CONFIDENCE,1"))
    }

    @Test
    fun `admin organization list is audited`() {
        val adminToken = obtainAccessToken("admin_governance", "admin_governance@example.com", UserRole.ADMIN)

        organizationRepository.save(
            Organization(
                name = "Governance Org",
                tokenHash = passwordEncoder.encode("governance-token"),
                tokenPreview = "gov_****",
                enabled = true,
            ),
        )

        mockMvc.perform(
            get("/api/admin/organizations")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)

        mockMvc.perform(
            get("/api/admin/access-audit-events")
                .header("Authorization", "Bearer $adminToken")
                .param("limit", "10")
                .param("actionType", AccessAuditActionType.ORGANIZATION_DIRECTORY_VIEWED.name),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].actorUsername").value("admin_governance"))
            .andExpect(jsonPath("$.data[0].actorRole").value("ADMIN"))
            .andExpect(jsonPath("$.data[0].actionType").value("ORGANIZATION_DIRECTORY_VIEWED"))
            .andExpect(jsonPath("$.data[0].targetType").value("ORGANIZATION_DIRECTORY"))
            .andExpect(jsonPath("$.data[0].targetId").value("00000000-0000-0000-0000-000000000001"))
    }

    @Test
    fun `admin skill dictionary list is audited`() {
        val adminToken = obtainAccessToken("admin_skills_audit", "admin_skills_audit@example.com", UserRole.ADMIN)

        skillDictionaryRepository.save(
            SkillDictionaryEntry(
                name = "Vue 3",
                category = "frontend",
                aliases = listOf("Vue3"),
            ),
        )

        mockMvc.perform(
            get("/api/admin/skills")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))

        mockMvc.perform(
            get("/api/admin/access-audit-events")
                .header("Authorization", "Bearer $adminToken")
                .param("limit", "10")
                .param("actionType", AccessAuditActionType.SKILL_DICTIONARY_VIEWED.name),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].actorUsername").value("admin_skills_audit"))
            .andExpect(jsonPath("$.data[0].actorRole").value("ADMIN"))
            .andExpect(jsonPath("$.data[0].actionType").value("SKILL_DICTIONARY_VIEWED"))
            .andExpect(jsonPath("$.data[0].targetType").value("SKILL_DICTIONARY"))
            .andExpect(jsonPath("$.data[0].targetId").value(AccessAuditService.SKILL_DICTIONARY_TARGET_ID.toString()))
    }

    @Test
    fun `admin organization governance writes are audited`() {
        val adminToken = obtainAccessToken("admin_org_writer", "admin_org_writer@example.com", UserRole.ADMIN)

        val createResponse = mockMvc.perform(
            post("/api/admin/organizations")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("name" to "Governed Org"))),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.organization.name").value("Governed Org"))
            .andReturn()

        val organizationId = objectMapper.readTree(createResponse.response.contentAsString)
            .path("data")
            .path("organization")
            .path("id")
            .asText()

        mockMvc.perform(
            put("/api/admin/organizations/{organizationId}", organizationId)
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("name" to "Governed Org Updated", "enabled" to true))),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.name").value("Governed Org Updated"))

        mockMvc.perform(
            post("/api/admin/organizations/{organizationId}/regenerate-token", organizationId)
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.organization.id").value(organizationId))
            .andExpect(jsonPath("$.data.generatedToken").isNotEmpty)

        mockMvc.perform(
            get("/api/admin/access-audit-events")
                .header("Authorization", "Bearer $adminToken")
                .param("limit", "10")
                .param("actionType", AccessAuditActionType.ORGANIZATION_CREATED.name),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].actorUsername").value("admin_org_writer"))
            .andExpect(jsonPath("$.data[0].actionType").value("ORGANIZATION_CREATED"))
            .andExpect(jsonPath("$.data[0].targetType").value("ORGANIZATION_DIRECTORY"))
            .andExpect(jsonPath("$.data[0].targetId").value(organizationId))

        mockMvc.perform(
            get("/api/admin/access-audit-events")
                .header("Authorization", "Bearer $adminToken")
                .param("limit", "10")
                .param("actionType", AccessAuditActionType.ORGANIZATION_UPDATED.name),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].actorUsername").value("admin_org_writer"))
            .andExpect(jsonPath("$.data[0].actionType").value("ORGANIZATION_UPDATED"))
            .andExpect(jsonPath("$.data[0].targetType").value("ORGANIZATION_DIRECTORY"))
            .andExpect(jsonPath("$.data[0].targetId").value(organizationId))

        mockMvc.perform(
            get("/api/admin/access-audit-events")
                .header("Authorization", "Bearer $adminToken")
                .param("limit", "10")
                .param("actionType", AccessAuditActionType.ORGANIZATION_TOKEN_REGENERATED.name),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].actorUsername").value("admin_org_writer"))
            .andExpect(jsonPath("$.data[0].actionType").value("ORGANIZATION_TOKEN_REGENERATED"))
            .andExpect(jsonPath("$.data[0].targetType").value("ORGANIZATION_DIRECTORY"))
            .andExpect(jsonPath("$.data[0].targetId").value(organizationId))
    }

    @Test
    fun `admin access audit list exposes sensitive field tags`() {
        val adminToken = obtainAccessToken("admin_sensitive_audit", "admin_sensitive_audit@example.com", UserRole.ADMIN)
        val resumeId = java.util.UUID.randomUUID()

        accessAuditEventRepository.save(
            AccessAuditEvent(
                actorUsername = "reviewer",
                actorRole = AccessAuditActorRole.HR,
                actionType = AccessAuditActionType.SENSITIVE_FIELD_VIEWED,
                targetType = AccessAuditTargetType.RESUME,
                targetId = resumeId,
                sensitiveField = AccessAuditSensitiveField.BASIC_INFO_EMAIL,
            ),
        )

        mockMvc.perform(
            get("/api/admin/access-audit-events")
                .header("Authorization", "Bearer $adminToken")
                .param("limit", "10")
                .param("actionType", AccessAuditActionType.SENSITIVE_FIELD_VIEWED.name),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].actorUsername").value("reviewer"))
            .andExpect(jsonPath("$.data[0].actorRole").value("HR"))
            .andExpect(jsonPath("$.data[0].actionType").value("SENSITIVE_FIELD_VIEWED"))
            .andExpect(jsonPath("$.data[0].targetType").value("RESUME"))
            .andExpect(jsonPath("$.data[0].targetId").value(resumeId.toString()))
            .andExpect(jsonPath("$.data[0].sensitiveField").value("BASIC_INFO_EMAIL"))

        val accessAuditListViewedEvent = accessAuditEventRepository.findAll().single {
            it.actionType == AccessAuditActionType.ACCESS_AUDIT_EVENTS_VIEWED
        }
        assertEquals("admin_sensitive_audit", accessAuditListViewedEvent.actorUsername)
        assertEquals(AccessAuditActorRole.ADMIN, accessAuditListViewedEvent.actorRole)
        assertEquals(AccessAuditTargetType.SYSTEM_OVERVIEW, accessAuditListViewedEvent.targetType)
        assertEquals(AccessAuditService.SYSTEM_OVERVIEW_TARGET_ID, accessAuditListViewedEvent.targetId)
    }

    @Test
    fun `admin can view and export access audit summary`() {
        val adminToken = obtainAccessToken("admin_audit", "admin_audit@example.com", UserRole.ADMIN)

        accessAuditEventRepository.saveAll(
            listOf(
                AccessAuditEvent(
                    actorUsername = "candidate_summary",
                    actorRole = AccessAuditActorRole.CANDIDATE,
                    actionType = AccessAuditActionType.CANDIDATE_PROFILE_VIEWED,
                    targetType = AccessAuditTargetType.USER,
                    targetId = java.util.UUID.randomUUID(),
                ),
                AccessAuditEvent(
                    actorUsername = "hr_summary",
                    actorRole = AccessAuditActorRole.HR,
                    actionType = AccessAuditActionType.RESUME_STATUS_VIEWED,
                    targetType = AccessAuditTargetType.RESUME,
                    targetId = java.util.UUID.randomUUID(),
                ),
                AccessAuditEvent(
                    actorUsername = "admin_audit",
                    actorRole = AccessAuditActorRole.ADMIN,
                    actionType = AccessAuditActionType.ORGANIZATION_DIRECTORY_VIEWED,
                    targetType = AccessAuditTargetType.ORGANIZATION_DIRECTORY,
                    targetId = java.util.UUID.randomUUID(),
                ),
                AccessAuditEvent(
                    actorUsername = "admin_audit",
                    actorRole = AccessAuditActorRole.ADMIN,
                    actionType = AccessAuditActionType.PARSE_FAILURE_REVIEW_EVENTS_EXPORTED,
                    targetType = AccessAuditTargetType.RESUME,
                    targetId = java.util.UUID.randomUUID(),
                ),
                AccessAuditEvent(
                    actorUsername = "hr_sensitive",
                    actorRole = AccessAuditActorRole.HR,
                    actionType = AccessAuditActionType.SENSITIVE_FIELD_VIEWED,
                    targetType = AccessAuditTargetType.RESUME,
                    targetId = java.util.UUID.randomUUID(),
                    sensitiveField = AccessAuditSensitiveField.BASIC_INFO_EMAIL,
                ),
            ),
        )

        mockMvc.perform(
            get("/api/admin/access-audit-events/summary")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.totalEvents").value(5))
            .andExpect(jsonPath("$.data.firstEventAt").isNotEmpty)
            .andExpect(jsonPath("$.data.lastEventAt").isNotEmpty)
            .andExpect(jsonPath("$.data.actionCounts.length()").value(5))
            .andExpect(jsonPath("$.data.actionCounts[?(@.label=='CANDIDATE_PROFILE_VIEWED')].value").value(contains(1)))
            .andExpect(jsonPath("$.data.actionCounts[?(@.label=='RESUME_STATUS_VIEWED')].value").value(contains(1)))
            .andExpect(jsonPath("$.data.actionCounts[?(@.label=='ORGANIZATION_DIRECTORY_VIEWED')].value").value(contains(1)))
            .andExpect(jsonPath("$.data.actionCounts[?(@.label=='PARSE_FAILURE_REVIEW_EVENTS_EXPORTED')].value").value(contains(1)))
            .andExpect(jsonPath("$.data.actionCounts[?(@.label=='SENSITIVE_FIELD_VIEWED')].value").value(contains(1)))
            .andExpect(jsonPath("$.data.actorRoleCounts.length()").value(3))
            .andExpect(jsonPath("$.data.actorRoleCounts[?(@.label=='HR')].value").value(contains(2)))
            .andExpect(jsonPath("$.data.actorRoleCounts[?(@.label=='CANDIDATE')].value").value(contains(1)))
            .andExpect(jsonPath("$.data.actorRoleCounts[?(@.label=='ADMIN')].value").value(contains(2)))
            .andExpect(jsonPath("$.data.targetTypeCounts.length()").value(3))
            .andExpect(jsonPath("$.data.targetTypeCounts[?(@.label=='USER')].value").value(contains(1)))
            .andExpect(jsonPath("$.data.targetTypeCounts[?(@.label=='RESUME')].value").value(contains(3)))
            .andExpect(jsonPath("$.data.targetTypeCounts[?(@.label=='ORGANIZATION_DIRECTORY')].value").value(contains(1)))
            .andExpect(jsonPath("$.data.sensitiveFieldCounts.length()").value(1))
            .andExpect(jsonPath("$.data.sensitiveFieldCounts[?(@.label=='BASIC_INFO_EMAIL')].value").value(contains(1)))

        val exportResponse = mockMvc.perform(
            get("/api/admin/access-audit-events/summary/export")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andReturn()

        assertTrue(exportResponse.response.contentType?.startsWith("text/csv") == true)
        assertTrue(exportResponse.response.getHeader("Content-Disposition")!!.contains("access-audit-summary.csv"))
        assertTrue(exportResponse.response.contentAsString.contains("dimension,label,value"))
        assertTrue(exportResponse.response.contentAsString.contains("meta,totalEvents,6"))
        assertTrue(exportResponse.response.contentAsString.contains("actionType,CANDIDATE_PROFILE_VIEWED,1"))
        assertTrue(exportResponse.response.contentAsString.contains("actionType,ACCESS_AUDIT_SUMMARY_VIEWED,1"))
        assertTrue(exportResponse.response.contentAsString.contains("actorRole,ADMIN,3"))
        assertTrue(exportResponse.response.contentAsString.contains("targetType,RESUME,3"))
        assertTrue(exportResponse.response.contentAsString.contains("sensitiveField,BASIC_INFO_EMAIL,1"))

        val summaryViewedEvent = accessAuditEventRepository.findAll().single {
            it.actionType == AccessAuditActionType.ACCESS_AUDIT_SUMMARY_VIEWED
        }
        assertEquals("admin_audit", summaryViewedEvent.actorUsername)
        assertEquals(AccessAuditActorRole.ADMIN, summaryViewedEvent.actorRole)
        assertEquals(AccessAuditTargetType.SYSTEM_OVERVIEW, summaryViewedEvent.targetType)
        assertEquals(AccessAuditService.SYSTEM_OVERVIEW_TARGET_ID, summaryViewedEvent.targetId)

        val summaryExportedEvent = accessAuditEventRepository.findAll().single {
            it.actionType == AccessAuditActionType.ACCESS_AUDIT_SUMMARY_EXPORTED
        }
        assertEquals("admin_audit", summaryExportedEvent.actorUsername)
        assertEquals(AccessAuditActorRole.ADMIN, summaryExportedEvent.actorRole)
        assertEquals(AccessAuditTargetType.SYSTEM_OVERVIEW, summaryExportedEvent.targetType)
        assertEquals(AccessAuditService.SYSTEM_OVERVIEW_TARGET_ID, summaryExportedEvent.targetId)
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

        mockMvc.perform(
            get("/api/admin/access-audit-events")
                .header("Authorization", "Bearer $adminToken")
                .param("limit", "10")
                .param("actionType", AccessAuditActionType.SKILL_CREATED.name),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].actorUsername").value("admin_skill"))
            .andExpect(jsonPath("$.data[0].actorRole").value("ADMIN"))
            .andExpect(jsonPath("$.data[0].actionType").value("SKILL_CREATED"))
            .andExpect(jsonPath("$.data[0].targetType").value("SKILL_DICTIONARY"))
            .andExpect(jsonPath("$.data[0].targetId").value(skillId))

        mockMvc.perform(
            get("/api/admin/access-audit-events")
                .header("Authorization", "Bearer $adminToken")
                .param("limit", "10")
                .param("actionType", AccessAuditActionType.SKILL_UPDATED.name),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].actorUsername").value("admin_skill"))
            .andExpect(jsonPath("$.data[0].actorRole").value("ADMIN"))
            .andExpect(jsonPath("$.data[0].actionType").value("SKILL_UPDATED"))
            .andExpect(jsonPath("$.data[0].targetType").value("SKILL_DICTIONARY"))
            .andExpect(jsonPath("$.data[0].targetId").value(skillId))
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