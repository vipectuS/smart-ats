package com.smartats.backend.service

import com.smartats.backend.domain.AccessAuditActionType
import com.smartats.backend.domain.AdminParseFailureReviewStatus
import com.smartats.backend.domain.SkillDictionaryEntry
import com.smartats.backend.domain.UserRole
import com.smartats.backend.dto.admin.AccessAuditEventResponse
import com.smartats.backend.dto.admin.AccessAuditSummaryResponse
import com.smartats.backend.dto.admin.AdminDistributionItemResponse
import com.smartats.backend.dto.admin.AdminOverviewResponse
import com.smartats.backend.dto.admin.AdminOverviewTotalsResponse
import com.smartats.backend.dto.admin.AdminParseFailureBatchActionResponse
import com.smartats.backend.dto.admin.AdminParseFailureResponse
import com.smartats.backend.dto.admin.AdminParseFailureSummaryResponse
import com.smartats.backend.dto.admin.AdminParseFailureReviewEventResponse
import com.smartats.backend.dto.admin.AdminSkillResponse
import com.smartats.backend.dto.admin.AdminSkillUpsertRequest
import com.smartats.backend.dto.resume.ResumeParseTriggerResponse
import com.smartats.backend.dto.resume.parseFailureMetadata
import com.smartats.backend.exception.BadRequestException
import com.smartats.backend.exception.DuplicateResourceException
import com.smartats.backend.exception.ResourceNotFoundException
import com.smartats.backend.repository.AdminParseFailureReviewEventRepository
import com.smartats.backend.repository.JobRepository
import com.smartats.backend.repository.ResumeRepository
import com.smartats.backend.repository.SkillDictionaryRepository
import com.smartats.backend.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AdminService(
    private val userRepository: UserRepository,
    private val jobRepository: JobRepository,
    private val resumeRepository: ResumeRepository,
    private val adminParseFailureReviewEventRepository: AdminParseFailureReviewEventRepository,
    private val skillDictionaryRepository: SkillDictionaryRepository,
    private val resumeService: ResumeService,
    private val accessAuditService: AccessAuditService,
) {

    @Transactional(readOnly = true)
    fun getOverview(): AdminOverviewResponse {
        return AdminOverviewResponse(
            totals = AdminOverviewTotalsResponse(
                totalUsers = userRepository.count(),
                totalJobs = jobRepository.count(),
                totalResumes = resumeRepository.count(),
                totalSkillEntries = skillDictionaryRepository.count(),
            ),
            usersByRole = UserRole.entries.map { role ->
                AdminDistributionItemResponse(label = role.name, value = userRepository.countByRole(role))
            },
            resumesByStatus = listOf(
                ResumeService.STATUS_PENDING_PARSE,
                ResumeService.STATUS_PARSING,
                ResumeService.STATUS_PARSED,
                ResumeService.STATUS_PARSE_FAILED,
            ).map { status ->
                AdminDistributionItemResponse(label = status, value = resumeRepository.countByStatus(status))
            },
            latestParseFailures = resumeRepository.findByStatusOrderByUpdatedAtDesc(
                ResumeService.STATUS_PARSE_FAILED,
                PageRequest.of(0, 5),
            ).map { resume ->
                mapParseFailure(resume)
            },
        )
    }

    @Transactional(readOnly = true)
    fun listSkills(): List<AdminSkillResponse> {
        return skillDictionaryRepository.findAllByOrderByEnabledDescNameAsc()
            .map(AdminSkillResponse::from)
    }

    @Transactional
    fun createSkill(request: AdminSkillUpsertRequest): AdminSkillResponse {
        val normalizedName = normalizeName(request.name)
        if (skillDictionaryRepository.existsByNameIgnoreCase(normalizedName)) {
            throw DuplicateResourceException("Skill already exists")
        }

        val entry = skillDictionaryRepository.save(
            SkillDictionaryEntry(
                name = normalizedName,
                category = normalizeOptionalText(request.category),
                aliases = normalizeAliases(request.aliases, normalizedName),
                enabled = request.enabled,
            ),
        )
        return AdminSkillResponse.from(entry)
    }

    @Transactional
    fun updateSkill(skillId: UUID, request: AdminSkillUpsertRequest): AdminSkillResponse {
        val entry = skillDictionaryRepository.findById(skillId)
            .orElseThrow { ResourceNotFoundException("Skill entry not found") }
        val normalizedName = normalizeName(request.name)

        if (skillDictionaryRepository.existsByIdNotAndNameIgnoreCase(skillId, normalizedName)) {
            throw DuplicateResourceException("Skill already exists")
        }

        entry.name = normalizedName
        entry.category = normalizeOptionalText(request.category)
        entry.aliases = normalizeAliases(request.aliases, normalizedName)
        entry.enabled = request.enabled
        return AdminSkillResponse.from(skillDictionaryRepository.save(entry))
    }

    @Transactional(readOnly = true)
    fun listParseFailures(limit: Int, reviewStatus: AdminParseFailureReviewStatus?): List<AdminParseFailureResponse> {
        val normalizedLimit = limit.coerceIn(1, 100)
        val pageable = PageRequest.of(0, normalizedLimit)
        val resumes = if (reviewStatus == null) {
            resumeRepository.findByStatusOrderByUpdatedAtDesc(
                ResumeService.STATUS_PARSE_FAILED,
                pageable,
            )
        } else {
            resumeRepository.findByStatusAndAdminReviewStatusOrderByUpdatedAtDesc(
                ResumeService.STATUS_PARSE_FAILED,
                reviewStatus,
                pageable,
            )
        }
        return resumes.map(::mapParseFailure)
    }

    @Transactional(readOnly = true)
    fun getParseFailureSummary(): AdminParseFailureSummaryResponse {
        val failures = resumeRepository.findByStatus(ResumeService.STATUS_PARSE_FAILED)
        val reviewStatusCounts = AdminParseFailureReviewStatus.entries.map { reviewStatus ->
            AdminDistributionItemResponse(
                label = reviewStatus.name,
                value = failures.count { it.adminReviewStatus == reviewStatus }.toLong(),
            )
        }
        val failureCodeCounts = failures
            .groupingBy { parseFailureMetadata(it.parseFailureReason).code ?: "UNCLASSIFIED" }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .map { AdminDistributionItemResponse(label = it.key, value = it.value.toLong()) }

        return AdminParseFailureSummaryResponse(
            totalFailures = failures.size.toLong(),
            firstFailureAt = failures.minByOrNull { it.updatedAt }?.updatedAt,
            lastFailureAt = failures.maxByOrNull { it.updatedAt }?.updatedAt,
            reviewStatusCounts = reviewStatusCounts,
            failureCodeCounts = failureCodeCounts,
        )
    }

    @Transactional(readOnly = true)
    fun exportParseFailureSummaryCsv(): String {
        val summary = getParseFailureSummary()
        return buildList {
            add("dimension,label,value")
            add("meta,totalFailures,${summary.totalFailures}")
            add("meta,firstFailureAt,${summary.firstFailureAt ?: ""}")
            add("meta,lastFailureAt,${summary.lastFailureAt ?: ""}")
            summary.reviewStatusCounts.forEach { add("reviewStatus,${it.label},${it.value}") }
            summary.failureCodeCounts.forEach { add("failureCode,${it.label},${it.value}") }
        }.joinToString("\n")
    }

    @Transactional
    fun updateParseFailureReview(
        resumeId: UUID,
        adminUsername: String,
        note: String?,
        reviewStatus: AdminParseFailureReviewStatus?,
    ): AdminParseFailureResponse {
        val resume = resumeService.updateAdminReview(resumeId, adminUsername, note, reviewStatus)
        return mapParseFailure(resume)
    }

    @Transactional
    fun retryParseFailure(resumeId: UUID, adminUsername: String, note: String?): ResumeParseTriggerResponse {
        return resumeService.retryFailedResumeAsAdmin(resumeId, adminUsername, note)
    }

    @Transactional
    fun updateParseFailureReviews(
        resumeIds: List<UUID>,
        adminUsername: String,
        note: String?,
        reviewStatus: AdminParseFailureReviewStatus?,
    ): AdminParseFailureBatchActionResponse {
        val normalizedResumeIds = normalizeBatchResumeIds(resumeIds)
        normalizedResumeIds.forEach { resumeId ->
            resumeService.updateAdminReview(resumeId, adminUsername, note, reviewStatus)
        }
        return AdminParseFailureBatchActionResponse(
            processedCount = normalizedResumeIds.size,
            resumeIds = normalizedResumeIds,
            reviewStatus = reviewStatus,
            queued = false,
        )
    }

    @Transactional
    fun retryParseFailures(
        resumeIds: List<UUID>,
        adminUsername: String,
        note: String?,
    ): AdminParseFailureBatchActionResponse {
        val normalizedResumeIds = normalizeBatchResumeIds(resumeIds)
        normalizedResumeIds.forEach { resumeId ->
            resumeService.retryFailedResumeAsAdmin(resumeId, adminUsername, note)
        }
        return AdminParseFailureBatchActionResponse(
            processedCount = normalizedResumeIds.size,
            resumeIds = normalizedResumeIds,
            reviewStatus = AdminParseFailureReviewStatus.APPROVED_FOR_RETRY,
            queued = true,
        )
    }

    @Transactional(readOnly = true)
    fun listParseFailureReviewEvents(resumeId: UUID, limit: Int): List<AdminParseFailureReviewEventResponse> {
        return fetchParseFailureReviewEvents(resumeId, limit)
            .map(AdminParseFailureReviewEventResponse::from)
    }

    @Transactional(readOnly = true)
    fun exportParseFailureReviewEventsCsv(resumeId: UUID, limit: Int): String {
        val events = fetchParseFailureReviewEvents(resumeId, limit)
        val header = "createdAt,adminUsername,actionType,previousReviewStatus,nextReviewStatus,resumeStatusAfterAction,note"
        val rows = events.map { event ->
            listOf(
                event.createdAt.toString(),
                event.adminUsername,
                event.actionType.name,
                event.previousReviewStatus.name,
                event.nextReviewStatus.name,
                event.resumeStatusAfterAction,
                escapeCsv(event.note.orEmpty()),
            ).joinToString(",") { value ->
                if (value == event.note.orEmpty()) value else escapeCsv(value)
            }
        }
        return buildList {
            add(header)
            addAll(rows)
        }.joinToString("\n")
    }

    @Transactional(readOnly = true)
    fun listAccessAuditEvents(limit: Int, actionType: AccessAuditActionType?): List<AccessAuditEventResponse> {
        return accessAuditService.listEvents(limit, actionType)
    }

    @Transactional(readOnly = true)
    fun getAccessAuditSummary(): AccessAuditSummaryResponse {
        return accessAuditService.summarizeEvents()
    }

    @Transactional(readOnly = true)
    fun exportAccessAuditSummaryCsv(): String {
        return accessAuditService.exportSummaryCsv()
    }

    private fun mapParseFailure(resume: com.smartats.backend.domain.Resume): AdminParseFailureResponse {
        val parseFailure = parseFailureMetadata(resume.parseFailureReason)
        return AdminParseFailureResponse(
            resumeId = requireNotNull(resume.id),
            ownerUsername = resume.ownerUser?.username,
            sourceFileName = resume.browserPreprocessedPayload?.get("sourceFileName") as? String,
            rawContentReference = resume.rawContentReference,
            parseFailureCode = parseFailure.code,
            reason = parseFailure.reason,
            adminReviewNote = resume.adminReviewNote,
            reviewStatus = resume.adminReviewStatus,
            reviewedByUsername = resume.adminReviewedBy,
            reviewedAt = resume.adminReviewedAt,
            updatedAt = resume.updatedAt,
        )
    }

    private fun normalizeName(value: String): String {
        return value.trim().ifBlank { throw BadRequestException("Skill name is required") }
    }

    private fun fetchParseFailureReviewEvents(resumeId: UUID, limit: Int) = run {
        if (!resumeRepository.existsById(resumeId)) {
            throw ResourceNotFoundException("Resume not found")
        }
        val normalizedLimit = limit.coerceIn(1, 100)
        adminParseFailureReviewEventRepository.findByResumeIdOrderByCreatedAtDesc(
            resumeId,
            PageRequest.of(0, normalizedLimit),
        )
    }

    private fun escapeCsv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    private fun normalizeOptionalText(value: String?): String? {
        return value?.trim()?.ifBlank { null }
    }

    private fun normalizeAliases(aliases: List<String>, normalizedName: String): List<String> {
        return aliases
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filterNot { it.equals(normalizedName, ignoreCase = true) }
            .distinctBy { it.lowercase() }
    }

    private fun normalizeBatchResumeIds(resumeIds: List<UUID>): List<UUID> {
        val normalizedResumeIds = resumeIds.distinct()
        if (normalizedResumeIds.isEmpty()) {
            throw BadRequestException("At least one resumeId is required")
        }
        if (normalizedResumeIds.size > 50) {
            throw BadRequestException("A batch can contain at most 50 resumeIds")
        }
        return normalizedResumeIds
    }
}