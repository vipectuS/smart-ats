package com.smartats.backend.service

import com.smartats.backend.domain.AdminParseFailureReviewStatus
import com.smartats.backend.domain.AdminParseFailureReviewActionType
import com.smartats.backend.domain.AdminParseFailureReviewEvent
import com.smartats.backend.config.InternalCallbackProperties
import com.smartats.backend.config.ResumeQueueProperties
import com.smartats.backend.domain.Resume
import com.smartats.backend.domain.User
import com.smartats.backend.domain.UserRole
import com.smartats.backend.dto.resume.ResumeParseFailedRequest
import com.smartats.backend.dto.resume.ResumeParsedResultRequest
import com.smartats.backend.dto.resume.CreateResumeRequest
import com.smartats.backend.dto.resume.ResumeParseTriggerResponse
import com.smartats.backend.dto.resume.ResumeResponse
import com.smartats.backend.dto.resume.ResumeStatusResponse
import com.smartats.backend.dto.resume.composeParseFailureValue
import com.smartats.backend.dto.candidate.CandidateResumeDetailResponse
import com.smartats.backend.dto.candidate.CandidateResumeSummaryResponse
import com.smartats.backend.exception.ApiErrorCode
import com.smartats.backend.exception.BadRequestException
import com.smartats.backend.exception.ResourceNotFoundException
import com.smartats.backend.exception.InvalidCredentialsException
import com.smartats.backend.queue.ExternalContentReference
import com.smartats.backend.queue.ResumeParseMessage
import com.smartats.backend.queue.ResumeQueueProducer
import com.smartats.backend.repository.CandidateProfileRepository
import com.smartats.backend.repository.AdminParseFailureReviewEventRepository
import com.smartats.backend.repository.ResumeRepository
import com.smartats.backend.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import com.smartats.backend.dto.PageResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

@Service
class ResumeService(
    private val resumeRepository: ResumeRepository,
    private val userRepository: UserRepository,
    private val candidateProfileRepository: CandidateProfileRepository,
    private val adminParseFailureReviewEventRepository: AdminParseFailureReviewEventRepository,
    private val resumeQueueProducer: ResumeQueueProducer,
    private val resumeQueueProperties: ResumeQueueProperties,
    private val internalCallbackProperties: InternalCallbackProperties,
    private val embeddingService: EmbeddingService,
    private val recommendationRefreshTrigger: RecommendationRefreshTrigger,
) {

    companion object {
        const val STATUS_PENDING_PARSE = "PENDING_PARSE"
        const val STATUS_PARSING = "PARSING"
        const val STATUS_PARSED = "PARSED"
        const val STATUS_PARSE_FAILED = "PARSE_FAILED"
    }

    @Transactional
    fun createResume(request: CreateResumeRequest): ResumeResponse {
        val resume = Resume(
            candidateName = request.candidateName?.trim(),
            contactInfo = request.contactInfo?.trim(),
            rawContentReference = request.rawContentReference.trim(),
            browserPreprocessedPayload = request.browserPreprocessedPayloadAsMap(),
            parsedData = request.parsedData,
            parseFailureReason = null,
            status = STATUS_PENDING_PARSE,
        )

        return ResumeResponse.from(resumeRepository.save(resume))
    }

    @Transactional
    fun createResumeForCandidate(username: String, request: CreateResumeRequest): ResumeResponse {
        val user = userRepository.findByUsername(username)
            .orElseThrow { ResourceNotFoundException("User not found") }

        val savedResume = resumeRepository.save(
            Resume(
            candidateName = request.candidateName?.trim() ?: user.username,
            contactInfo = request.contactInfo?.trim() ?: user.email,
            rawContentReference = request.rawContentReference.trim(),
            browserPreprocessedPayload = request.browserPreprocessedPayloadAsMap(),
            parsedData = request.parsedData,
            parseFailureReason = null,
            ownerUser = user,
            status = STATUS_PENDING_PARSE,
            ),
        )

        publishParseMessage(savedResume)
        return ResumeResponse.from(savedResume)
    }

    @Transactional(readOnly = true)
    fun listResumes(page: Int, size: Int): PageResponse<ResumeResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val pageResult = resumeRepository.findAll(pageable)
        return PageResponse.from(pageResult.map { ResumeResponse.from(it) })
    }

    @Transactional
    fun triggerParse(resumeId: UUID): ResumeParseTriggerResponse {
        val resume = getResumeEntity(resumeId)
        publishParseMessage(resume)

        return ResumeParseTriggerResponse(
            resumeId = requireNotNull(resume.id),
            status = resume.status,
            queued = true,
            channel = resumeQueueProperties.channel,
        )
    }

    @Transactional(readOnly = true)
    fun listCandidateResumes(username: String): List<CandidateResumeSummaryResponse> {
        val candidate = getCandidateUser(username)
        return resumeRepository.findByOwnerUserIdOrderByUpdatedAtDesc(requireNotNull(candidate.id))
            .map { CandidateResumeSummaryResponse.from(it) }
    }

    @Transactional(readOnly = true)
    fun getCandidateResume(username: String, resumeId: UUID): CandidateResumeDetailResponse {
        val candidate = getCandidateUser(username)
        val resume = resumeRepository.findByIdAndOwnerUserId(resumeId, requireNotNull(candidate.id))
            ?: throw ResourceNotFoundException("Resume not found")
        return CandidateResumeDetailResponse.from(resume)
    }

    @Transactional
    fun triggerCandidateResumeParse(username: String, resumeId: UUID): ResumeParseTriggerResponse {
        val candidate = getCandidateUser(username)
        val resume = resumeRepository.findByIdAndOwnerUserId(resumeId, requireNotNull(candidate.id))
            ?: throw ResourceNotFoundException("Resume not found")

        resume.status = STATUS_PENDING_PARSE
        resume.parseFailureReason = null
        val savedResume = resumeRepository.save(resume)
        publishParseMessage(savedResume)

        return ResumeParseTriggerResponse(
            resumeId = requireNotNull(savedResume.id),
            status = savedResume.status,
            queued = true,
            channel = resumeQueueProperties.channel,
        )
    }

    @Transactional
    fun updateAdminReview(
        resumeId: UUID,
        adminUsername: String,
        note: String?,
        reviewStatus: AdminParseFailureReviewStatus?,
    ): Resume {
        val resume = getResumeEntity(resumeId)
        ensureFailedResume(resume)
        val previousReviewStatus = resume.adminReviewStatus
        applyAdminReview(resume, adminUsername, note, reviewStatus)
        val savedResume = resumeRepository.save(resume)
        appendAdminReviewEvent(
            resume = savedResume,
            adminUsername = adminUsername,
            actionType = AdminParseFailureReviewActionType.REVIEW_SAVED,
            note = savedResume.adminReviewNote,
            previousReviewStatus = previousReviewStatus,
            nextReviewStatus = savedResume.adminReviewStatus,
        )
        return savedResume
    }

    @Transactional
    fun retryFailedResumeAsAdmin(resumeId: UUID, adminUsername: String, note: String?): ResumeParseTriggerResponse {
        val resume = getResumeEntity(resumeId)
        ensureFailedResume(resume)

        val previousReviewStatus = resume.adminReviewStatus
        applyAdminReview(resume, adminUsername, note, AdminParseFailureReviewStatus.APPROVED_FOR_RETRY)
        resume.status = STATUS_PENDING_PARSE
        resume.parseFailureReason = null
        resume.parsedData = null
        resume.embedding = null
        resume.runtimeEmbedding = null
        val savedResume = resumeRepository.save(resume)
        appendAdminReviewEvent(
            resume = savedResume,
            adminUsername = adminUsername,
            actionType = AdminParseFailureReviewActionType.RETRY_QUEUED,
            note = savedResume.adminReviewNote,
            previousReviewStatus = previousReviewStatus,
            nextReviewStatus = savedResume.adminReviewStatus,
        )
        publishParseMessage(savedResume)

        return ResumeParseTriggerResponse(
            resumeId = requireNotNull(savedResume.id),
            status = savedResume.status,
            queued = true,
            channel = resumeQueueProperties.channel,
        )
    }

    private fun publishParseMessage(resume: Resume) {
        val message = ResumeParseMessage(
            resumeId = requireNotNull(resume.id),
            rawContentReference = resume.rawContentReference,
            browserPreprocessedPayload = resume.browserPreprocessedPayload,
            externalContentReferences = resolveExternalContentReferences(resume),
            requestedAt = Instant.now(),
        )

        publishAfterCommit(message)
    }

    private fun publishAfterCommit(message: ResumeParseMessage) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            resumeQueueProducer.publish(message)
            return
        }

        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCommit() {
                    resumeQueueProducer.publish(message)
                }
            },
        )
    }

    private fun scheduleResumeRecommendationRefresh(resumeId: UUID) {
        runAfterCommit {
            recommendationRefreshTrigger.refreshResumeRecommendations(resumeId)
        }
    }

    private fun scheduleResumeRecommendationClear(resumeId: UUID) {
        runAfterCommit {
            recommendationRefreshTrigger.clearResumeRecommendations(resumeId)
        }
    }

    private fun runAfterCommit(task: () -> Unit) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            task()
            return
        }

        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCommit() {
                    task()
                }
            },
        )
    }

    private fun resolveExternalContentReferences(resume: Resume): List<ExternalContentReference> {
        val ownerUserId = resume.ownerUser?.id ?: return emptyList()
        val profile = candidateProfileRepository.findByUserId(ownerUserId).orElse(null) ?: return emptyList()

        return buildList {
            profile.githubUrl?.trim()?.takeIf { it.isNotEmpty() }?.let {
                add(ExternalContentReference(sourceType = "github", url = it))
            }
            profile.portfolioUrl?.trim()?.takeIf { it.isNotEmpty() }?.let {
                add(ExternalContentReference(sourceType = "portfolio", url = it))
            }
        }
    }

    @Transactional(readOnly = true)
    fun getResume(resumeId: UUID): ResumeResponse {
        return ResumeResponse.from(getResumeEntity(resumeId))
    }

    @Transactional(readOnly = true)
    fun getResumeStatus(resumeId: UUID): ResumeStatusResponse {
        return ResumeStatusResponse.from(getResumeEntity(resumeId))
    }

    @Transactional
    fun markParsing(resumeId: UUID) {
        val resume = getResumeEntity(resumeId)
        resume.status = STATUS_PARSING
        resume.parseFailureReason = null
        resumeRepository.save(resume)
    }

    @Transactional
    fun markParsed(resumeId: UUID, parsedData: Map<String, Any>) {
        val resume = getResumeEntity(resumeId)
        resume.status = STATUS_PARSED
        resume.parsedData = parsedData
        resume.parseFailureReason = null
        val savedResume = resumeRepository.save(resume)
        persistResumeEmbedding(savedResume, parsedData)
        scheduleResumeRecommendationRefresh(requireNotNull(savedResume.id))
    }

    @Transactional
    fun applyParsedResult(
        resumeId: UUID,
        apiKey: String?,
        request: ResumeParsedResultRequest,
    ): ResumeStatusResponse {
        validateInternalApiKey(apiKey)
        if (request.parsedData.isEmpty()) {
            throw BadRequestException(
                message = "parsedData must not be empty",
                errorCode = ApiErrorCode.RESUME_PARSED_DATA_REQUIRED,
                retryable = true,
                userHint = "请确认 AI 回调携带了完整的 parsedData 后再重试。",
            )
        }

        val resume = getResumeEntity(resumeId)
        resume.status = STATUS_PARSED
        resume.parsedData = request.parsedData
        resume.parseFailureReason = null
        val savedResume = resumeRepository.save(resume)
        persistResumeEmbedding(savedResume, request.parsedData)
        scheduleResumeRecommendationRefresh(requireNotNull(savedResume.id))
        return ResumeStatusResponse.from(savedResume)
    }

    @Transactional
    fun markParseFailed(resumeId: UUID, reason: String? = null) {
        val resume = getResumeEntity(resumeId)
        resume.status = STATUS_PARSE_FAILED
        resetAdminReview(resume)
        resume.parsedData = null
        resume.embedding = null
        resume.runtimeEmbedding = null
        resume.parseFailureReason = reason?.trim()?.ifBlank { null }
        val savedResume = resumeRepository.save(resume)
        scheduleResumeRecommendationClear(requireNotNull(savedResume.id))
    }

    @Transactional
    fun applyParseFailedResult(
        resumeId: UUID,
        apiKey: String?,
        request: ResumeParseFailedRequest,
    ): ResumeStatusResponse {
        validateInternalApiKey(apiKey)

        val resume = getResumeEntity(resumeId)
        if (resume.status == STATUS_PARSED) {
            throw BadRequestException(
                message = "Cannot mark a parsed resume as failed",
                errorCode = ApiErrorCode.RESUME_ALREADY_PARSED,
                retryable = false,
                userHint = "该简历已经完成解析，如需重试请重新发起解析流程。",
            )
        }

        resume.status = STATUS_PARSE_FAILED
        resetAdminReview(resume)
        resume.parsedData = null
        resume.embedding = null
        resume.runtimeEmbedding = null
        resume.parseFailureReason = composeParseFailureValue(request.failureCode, request.reason)
        val savedResume = resumeRepository.save(resume)
        scheduleResumeRecommendationClear(requireNotNull(savedResume.id))
        return ResumeStatusResponse.from(savedResume)
    }

    private fun persistResumeEmbedding(resume: Resume, parsedData: Map<String, Any>) {
        val embedding = embeddingService.generateResumeEmbedding(parsedData)
        val resumeId = requireNotNull(resume.id)
        if (embeddingService.shouldUseNativeVectorStorage()) {
            resumeRepository.updateEmbedding(resumeId, embedding)
            resume.embedding = embedding
        } else {
            resume.runtimeEmbedding = embedding
        }
        resume.runtimeEmbedding = embedding
    }

    private fun ensureFailedResume(resume: Resume) {
        if (resume.status != STATUS_PARSE_FAILED) {
            throw BadRequestException(
                message = "Only failed resumes can be manually reviewed or retried",
                errorCode = ApiErrorCode.PARSE_FAILURE_INVALID_STATE,
                retryable = false,
                userHint = "请仅对解析失败的简历执行人工复核或重试。",
            )
        }
    }

    private fun applyAdminReview(
        resume: Resume,
        adminUsername: String,
        note: String?,
        reviewStatus: AdminParseFailureReviewStatus?,
    ) {
        resume.adminReviewNote = note?.trim()?.ifBlank { null }
        resume.adminReviewedBy = adminUsername
        resume.adminReviewedAt = LocalDateTime.now()
        resume.adminReviewStatus = reviewStatus ?: resume.adminReviewStatus
    }

    private fun appendAdminReviewEvent(
        resume: Resume,
        adminUsername: String,
        actionType: AdminParseFailureReviewActionType,
        note: String?,
        previousReviewStatus: AdminParseFailureReviewStatus,
        nextReviewStatus: AdminParseFailureReviewStatus,
    ) {
        adminParseFailureReviewEventRepository.save(
            AdminParseFailureReviewEvent(
                resume = resume,
                adminUsername = adminUsername,
                actionType = actionType,
                note = note,
                previousReviewStatus = previousReviewStatus,
                nextReviewStatus = nextReviewStatus,
                resumeStatusAfterAction = resume.status,
            ),
        )
    }

    private fun resetAdminReview(resume: Resume) {
        resume.adminReviewNote = null
        resume.adminReviewedBy = null
        resume.adminReviewedAt = null
        resume.adminReviewStatus = AdminParseFailureReviewStatus.UNREVIEWED
    }

    private fun getResumeEntity(resumeId: UUID): Resume {
        return resumeRepository.findById(resumeId)
            .orElseThrow { ResourceNotFoundException("Resume not found") }
    }

    private fun getCandidateUser(username: String): User {
        val user = userRepository.findByUsername(username)
            .orElseThrow { ResourceNotFoundException("User not found") }
        if (user.role != UserRole.CANDIDATE) {
            throw BadRequestException(
                message = "Current user is not a candidate",
                errorCode = ApiErrorCode.CANDIDATE_ROLE_REQUIRED,
                retryable = false,
                userHint = "请使用候选人账号访问该功能。",
            )
        }
        return user
    }

    private fun validateInternalApiKey(apiKey: String?) {
        if (apiKey.isNullOrBlank() || apiKey != internalCallbackProperties.apiKey) {
            throw InvalidCredentialsException(
                message = "Invalid internal API key",
                errorCode = ApiErrorCode.INTERNAL_API_KEY_INVALID,
                retryable = false,
                userHint = "请检查 AI 服务与后端之间的内部回调密钥配置。",
            )
        }
    }
}