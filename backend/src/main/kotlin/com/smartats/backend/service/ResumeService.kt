package com.smartats.backend.service

import com.smartats.backend.config.InternalCallbackProperties
import com.smartats.backend.config.ResumeQueueProperties
import com.smartats.backend.domain.Resume
import com.smartats.backend.dto.resume.ResumeParseFailedRequest
import com.smartats.backend.dto.resume.ResumeParsedResultRequest
import com.smartats.backend.dto.resume.CreateResumeRequest
import com.smartats.backend.dto.resume.ResumeParseTriggerResponse
import com.smartats.backend.dto.resume.ResumeResponse
import com.smartats.backend.dto.resume.ResumeStatusResponse
import com.smartats.backend.exception.BadRequestException
import com.smartats.backend.exception.ResourceNotFoundException
import com.smartats.backend.exception.InvalidCredentialsException
import com.smartats.backend.queue.ResumeParseMessage
import com.smartats.backend.queue.ResumeQueueProducer
import com.smartats.backend.repository.ResumeRepository
import com.smartats.backend.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import com.smartats.backend.dto.PageResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class ResumeService(
    private val resumeRepository: ResumeRepository,
    private val userRepository: UserRepository,
    private val resumeQueueProducer: ResumeQueueProducer,
    private val resumeQueueProperties: ResumeQueueProperties,
    private val internalCallbackProperties: InternalCallbackProperties,
    private val embeddingService: EmbeddingService,
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

        val resume = Resume(
            candidateName = request.candidateName?.trim() ?: user.username,
            contactInfo = request.contactInfo?.trim() ?: user.email,
            rawContentReference = request.rawContentReference.trim(),
            parsedData = request.parsedData,
            parseFailureReason = null,
            ownerUser = user,
            status = STATUS_PENDING_PARSE,
        )

        return ResumeResponse.from(resumeRepository.save(resume))
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
        val message = ResumeParseMessage(
            resumeId = requireNotNull(resume.id),
            rawContentReference = resume.rawContentReference,
            requestedAt = Instant.now(),
        )

        resumeQueueProducer.publish(message)

        return ResumeParseTriggerResponse(
            resumeId = message.resumeId,
            status = resume.status,
            queued = true,
            channel = resumeQueueProperties.channel,
        )
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
    }

    @Transactional
    fun applyParsedResult(
        resumeId: UUID,
        apiKey: String?,
        request: ResumeParsedResultRequest,
    ): ResumeStatusResponse {
        validateInternalApiKey(apiKey)
        if (request.parsedData.isEmpty()) {
            throw BadRequestException("parsedData must not be empty")
        }

        val resume = getResumeEntity(resumeId)
        resume.status = STATUS_PARSED
        resume.parsedData = request.parsedData
        resume.parseFailureReason = null
        val savedResume = resumeRepository.save(resume)
        persistResumeEmbedding(savedResume, request.parsedData)
        return ResumeStatusResponse.from(savedResume)
    }

    @Transactional
    fun markParseFailed(resumeId: UUID, reason: String? = null) {
        val resume = getResumeEntity(resumeId)
        resume.status = STATUS_PARSE_FAILED
        resume.parsedData = null
        resume.embedding = null
        resume.parseFailureReason = reason?.trim()?.ifBlank { null }
        resumeRepository.save(resume)
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
            throw BadRequestException("Cannot mark a parsed resume as failed")
        }

        resume.status = STATUS_PARSE_FAILED
        resume.parsedData = null
        resume.embedding = null
        resume.parseFailureReason = request.reason?.trim()?.ifBlank { null }
        val savedResume = resumeRepository.save(resume)
        return ResumeStatusResponse.from(savedResume)
    }

    private fun persistResumeEmbedding(resume: Resume, parsedData: Map<String, Any>) {
        val embedding = embeddingService.generateResumeEmbedding(parsedData)
        val resumeId = requireNotNull(resume.id)
        if (embeddingService.shouldUseNativeVectorStorage()) {
            resumeRepository.updateEmbedding(resumeId, embedding)
        } else {
            resume.embedding = embedding
            resumeRepository.save(resume)
        }
        resume.embedding = embedding
    }

    private fun getResumeEntity(resumeId: UUID): Resume {
        return resumeRepository.findById(resumeId)
            .orElseThrow { ResourceNotFoundException("Resume not found") }
    }

    private fun validateInternalApiKey(apiKey: String?) {
        if (apiKey.isNullOrBlank() || apiKey != internalCallbackProperties.apiKey) {
            throw InvalidCredentialsException("Invalid internal API key")
        }
    }
}