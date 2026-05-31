package com.smartats.backend.service

import com.smartats.backend.domain.AccessAuditActionType
import com.smartats.backend.domain.AccessAuditActorRole
import com.smartats.backend.domain.AccessAuditEvent
import com.smartats.backend.domain.AccessAuditSensitiveField
import com.smartats.backend.domain.AccessAuditTargetType
import com.smartats.backend.dto.candidate.CandidateProfileResponse
import com.smartats.backend.dto.admin.AccessAuditEventResponse
import com.smartats.backend.dto.admin.AccessAuditSummaryResponse
import com.smartats.backend.dto.admin.AdminDistributionItemResponse
import com.smartats.backend.dto.job.JobApplicationReviewItemResponse
import com.smartats.backend.repository.AccessAuditEventRepository
import com.smartats.backend.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AccessAuditService(
    private val accessAuditEventRepository: AccessAuditEventRepository,
    private val userRepository: UserRepository,
) {

    companion object {
        val ORGANIZATION_DIRECTORY_TARGET_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val SYSTEM_OVERVIEW_TARGET_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000002")
        val SKILL_DICTIONARY_TARGET_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000003")
    }

    @Transactional
    fun recordAccess(
        authentication: Authentication?,
        actionType: AccessAuditActionType,
        targetType: AccessAuditTargetType,
        targetId: UUID,
        sensitiveField: AccessAuditSensitiveField? = null,
    ) {
        val actor = resolveActor(authentication)
        accessAuditEventRepository.save(
            AccessAuditEvent(
                actorUsername = actor.username,
                actorRole = actor.role,
                actionType = actionType,
                targetType = targetType,
                targetId = targetId,
                sensitiveField = sensitiveField,
            ),
        )
    }

    @Transactional
    fun recordSelfAccess(
        authentication: Authentication,
        actionType: AccessAuditActionType,
    ) {
        val actor = userRepository.findByUsername(authentication.name)
            .orElseThrow { IllegalStateException("Cannot resolve actor for audit event") }
        recordAccess(authentication, actionType, AccessAuditTargetType.USER, requireNotNull(actor.id))
    }

    @Transactional
    fun recordCurrentUserAccountSensitiveFieldAccess(authentication: Authentication) {
        val actor = userRepository.findByUsername(authentication.name)
            .orElseThrow { IllegalStateException("Cannot resolve actor for audit event") }
        recordSensitiveFieldAccess(
            authentication,
            AccessAuditTargetType.USER,
            requireNotNull(actor.id),
            listOf(AccessAuditSensitiveField.ACCOUNT_EMAIL),
        )
    }

    @Transactional
    fun recordOrganizationDirectoryAccess(
        authentication: Authentication?,
    ) {
        recordAccess(
            authentication,
            AccessAuditActionType.ORGANIZATION_DIRECTORY_VIEWED,
            AccessAuditTargetType.ORGANIZATION_DIRECTORY,
            ORGANIZATION_DIRECTORY_TARGET_ID,
        )
    }

    @Transactional
    fun recordAdminOverviewAccess(authentication: Authentication) {
        recordAccess(
            authentication,
            AccessAuditActionType.ADMIN_OVERVIEW_VIEWED,
            AccessAuditTargetType.SYSTEM_OVERVIEW,
            SYSTEM_OVERVIEW_TARGET_ID,
        )
    }

    @Transactional
    fun recordSkillDictionaryAccess(authentication: Authentication) {
        recordAccess(
            authentication,
            AccessAuditActionType.SKILL_DICTIONARY_VIEWED,
            AccessAuditTargetType.SKILL_DICTIONARY,
            SKILL_DICTIONARY_TARGET_ID,
        )
    }

    @Transactional
    fun recordRecommendationCandidateDetailsAccess(
        authentication: Authentication,
        resumeIds: Collection<UUID>,
    ) {
        resumeIds.distinct().forEach { resumeId ->
            recordAccess(
                authentication,
                AccessAuditActionType.RECOMMENDATION_CANDIDATE_DETAILS_VIEWED,
                AccessAuditTargetType.RESUME,
                resumeId,
            )
        }
    }

    @Transactional
    fun recordRecommendationJobDetailsAccess(
        authentication: Authentication,
        jobIds: Collection<UUID>,
    ) {
        jobIds.distinct().forEach { jobId ->
            recordAccess(
                authentication,
                AccessAuditActionType.RECOMMENDATION_JOB_DETAILS_VIEWED,
                AccessAuditTargetType.JOB,
                jobId,
            )
        }
    }

    @Transactional
    fun recordSensitiveFieldAccess(
        authentication: Authentication,
        targetType: AccessAuditTargetType,
        targetId: UUID,
        sensitiveFields: Collection<AccessAuditSensitiveField>,
    ) {
        sensitiveFields.distinct().forEach { sensitiveField ->
            recordAccess(
                authentication,
                AccessAuditActionType.SENSITIVE_FIELD_VIEWED,
                targetType,
                targetId,
                sensitiveField,
            )
        }
    }

    @Transactional
    fun recordResumeSensitiveFieldAccess(
        authentication: Authentication,
        resumeId: UUID,
        contactInfo: String?,
        parsedData: Map<String, Any>?,
    ) {
        recordSensitiveFieldAccess(
            authentication,
            AccessAuditTargetType.RESUME,
            resumeId,
            detectSensitiveFields(contactInfo, parsedData),
        )
    }

    @Transactional
    fun recordCandidateProfileSensitiveFieldAccess(
        authentication: Authentication,
        profileResponse: CandidateProfileResponse,
    ) {
        if (profileResponse.email.isNotBlank()) {
            recordSensitiveFieldAccess(
                authentication,
                AccessAuditTargetType.USER,
                profileResponse.userId,
                listOf(AccessAuditSensitiveField.ACCOUNT_EMAIL),
            )
        }

        profileResponse.latestResume?.let { latestResume ->
            recordResumeSensitiveFieldAccess(
                authentication,
                latestResume.resumeId,
                latestResume.contactInfo,
                latestResume.parsedData,
            )
        }
    }

    @Transactional
    fun recordJobApplicationSensitiveFieldAccess(
        authentication: Authentication,
        applications: Collection<JobApplicationReviewItemResponse>,
    ) {
        applications.forEach { application ->
            if (application.candidate.email.isNotBlank()) {
                recordSensitiveFieldAccess(
                    authentication,
                    AccessAuditTargetType.USER,
                    application.candidate.id,
                    listOf(AccessAuditSensitiveField.ACCOUNT_EMAIL),
                )
            }

            application.latestResume?.takeIf { !it.contactInfo.isNullOrBlank() }?.let { latestResume ->
                recordSensitiveFieldAccess(
                    authentication,
                    AccessAuditTargetType.RESUME,
                    latestResume.resumeId,
                    listOf(AccessAuditSensitiveField.CONTACT_INFO),
                )
            }
        }
    }

    @Transactional(readOnly = true)
    fun listEvents(limit: Int, actionType: AccessAuditActionType?): List<AccessAuditEventResponse> {
        val normalizedLimit = limit.coerceIn(1, 100)
        val pageable = PageRequest.of(0, normalizedLimit)
        val events = if (actionType == null) {
            accessAuditEventRepository.findAllByOrderByCreatedAtDesc(pageable)
        } else {
            accessAuditEventRepository.findByActionTypeOrderByCreatedAtDesc(actionType, pageable)
        }
        return events.map(AccessAuditEventResponse::from)
    }

    @Transactional(readOnly = true)
    fun summarizeEvents(): AccessAuditSummaryResponse {
        val events = accessAuditEventRepository.findAll()
        return AccessAuditSummaryResponse(
            totalEvents = events.size.toLong(),
            firstEventAt = events.minOfOrNull(AccessAuditEvent::createdAt),
            lastEventAt = events.maxOfOrNull(AccessAuditEvent::createdAt),
            actionCounts = AccessAuditActionType.entries.mapNotNull { actionType ->
                distributionItem(actionType.name, events.count { it.actionType == actionType }.toLong())
            },
            actorRoleCounts = AccessAuditActorRole.entries.mapNotNull { actorRole ->
                distributionItem(actorRole.name, events.count { it.actorRole == actorRole }.toLong())
            },
            targetTypeCounts = AccessAuditTargetType.entries.mapNotNull { targetType ->
                distributionItem(targetType.name, events.count { it.targetType == targetType }.toLong())
            },
            sensitiveFieldCounts = AccessAuditSensitiveField.entries.mapNotNull { sensitiveField ->
                distributionItem(sensitiveField.name, events.count { it.sensitiveField == sensitiveField }.toLong())
            },
        )
    }

    @Transactional(readOnly = true)
    fun exportSummaryCsv(): String {
        val summary = summarizeEvents()
        val rows = buildList {
            add("dimension,label,value")
            add("meta,totalEvents,${summary.totalEvents}")
            add("meta,firstEventAt,${summary.firstEventAt?.toString().orEmpty()}")
            add("meta,lastEventAt,${summary.lastEventAt?.toString().orEmpty()}")
            addAll(summary.actionCounts.map { "actionType,${it.label},${it.value}" })
            addAll(summary.actorRoleCounts.map { "actorRole,${it.label},${it.value}" })
            addAll(summary.targetTypeCounts.map { "targetType,${it.label},${it.value}" })
            addAll(summary.sensitiveFieldCounts.map { "sensitiveField,${it.label},${it.value}" })
        }
        return rows.joinToString("\n")
    }

    private fun distributionItem(label: String, value: Long): AdminDistributionItemResponse? {
        return value.takeIf { it > 0 }?.let { AdminDistributionItemResponse(label = label, value = it) }
    }

    private fun detectSensitiveFields(
        contactInfo: String?,
        parsedData: Map<String, Any>?,
    ): Set<AccessAuditSensitiveField> {
        val parsedBasicInfo = parsedData
            ?.get("basicInfo")
            ?.let { it as? Map<*, *> }
        val parsedEmail = parsedBasicInfo?.get("email") as? String
        val parsedPhone = parsedBasicInfo?.get("phone") as? String

        return buildSet {
            if (!contactInfo.isNullOrBlank()) {
                add(AccessAuditSensitiveField.CONTACT_INFO)
            }
            if (!parsedEmail.isNullOrBlank()) {
                add(AccessAuditSensitiveField.BASIC_INFO_EMAIL)
            }
            if (!parsedPhone.isNullOrBlank()) {
                add(AccessAuditSensitiveField.BASIC_INFO_PHONE)
            }
        }
    }

    private fun resolveActor(authentication: Authentication?): AccessAuditActor {
        if (authentication == null || authentication.authorities.any { it.authority == "ROLE_ANONYMOUS" }) {
            return AccessAuditActor(username = authentication?.name ?: "anonymousUser", role = AccessAuditActorRole.ANONYMOUS)
        }

        return when {
            authentication.authorities.any { it.authority == "ROLE_ADMIN" } -> AccessAuditActor(authentication.name, AccessAuditActorRole.ADMIN)
            authentication.authorities.any { it.authority == "ROLE_HR" } -> AccessAuditActor(authentication.name, AccessAuditActorRole.HR)
            authentication.authorities.any { it.authority == "ROLE_CANDIDATE" } -> AccessAuditActor(authentication.name, AccessAuditActorRole.CANDIDATE)
            else -> throw IllegalStateException("Cannot resolve role for audit event")
        }
    }

    private data class AccessAuditActor(
        val username: String,
        val role: AccessAuditActorRole,
    )
}