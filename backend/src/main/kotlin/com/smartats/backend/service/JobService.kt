package com.smartats.backend.service

import com.smartats.backend.domain.Job
import com.smartats.backend.domain.JobApplicationStatus
import com.smartats.backend.domain.UserRole
import com.smartats.backend.dto.PageResponse
import com.smartats.backend.dto.job.CreateJobRequest
import com.smartats.backend.dto.job.JobApplicationReviewItemResponse
import com.smartats.backend.dto.job.JobResponse
import com.smartats.backend.dto.job.UpdateJobApplicationReviewRequest
import com.smartats.backend.dto.job.UpdateJobRequest
import com.smartats.backend.dto.job.toJobApplicationReviewItem
import com.smartats.backend.exception.ResourceNotFoundException
import com.smartats.backend.repository.JobApplicationRepository
import com.smartats.backend.repository.JobRepository
import com.smartats.backend.repository.ResumeRepository
import com.smartats.backend.repository.UserRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class JobService(
    private val jobRepository: JobRepository,
    private val userRepository: UserRepository,
    private val jobApplicationRepository: JobApplicationRepository,
    private val resumeRepository: ResumeRepository,
    private val embeddingService: EmbeddingService,
) {

    @Transactional
    fun createJob(username: String, request: CreateJobRequest): JobResponse {
        val creator = userRepository.findByUsername(username)
            .orElseThrow { ResourceNotFoundException("User not found") }

        val job = Job(
            title = request.title.trim(),
            description = request.description.trim(),
            requirements = request.requirements,
            createdBy = creator,
        )

        val savedJob = jobRepository.save(job)
        val embedding = embeddingService.generateJobEmbedding(savedJob)
        persistJobEmbedding(savedJob, embedding)
        return JobResponse.from(savedJob)
    }

    @Transactional
    fun updateJob(username: String, jobId: UUID, request: UpdateJobRequest): JobResponse {
        val operator = getOperator(username)
        val job = getJobEntity(jobId)
        assertCanManageJob(operator.role, job.createdBy?.username, username, "Only the job creator or an admin can update this job")

        job.title = request.title.trim()
        job.description = request.description.trim()
        job.requirements = request.requirements

        val savedJob = jobRepository.save(job)
        val embedding = embeddingService.generateJobEmbedding(savedJob)
        persistJobEmbedding(savedJob, embedding)
        return JobResponse.from(savedJob)
    }

    @Transactional(readOnly = true)
    fun getJob(jobId: UUID): JobResponse {
        return JobResponse.from(getJobEntity(jobId))
    }

    @Transactional(readOnly = true)
    fun listJobs(page: Int, size: Int): PageResponse<JobResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val jobs = jobRepository.findAll(pageable).map(JobResponse::from)
        return PageResponse.from(jobs)
    }

    @Transactional(readOnly = true)
    fun listActiveApplications(username: String, jobId: UUID): List<JobApplicationReviewItemResponse> {
        val operator = getOperator(username)
        val job = getJobEntity(jobId)
        assertCanManageJob(operator.role, job.createdBy?.username, username, "Only the job creator or an admin can review applications for this job")

        return jobApplicationRepository.findByJobIdAndStatusNotOrderByUpdatedAtDesc(jobId, JobApplicationStatus.WITHDRAWN)
            .map { application ->
                val candidateId = requireNotNull(application.user.id)
                val latestResume = resumeRepository.findTopByOwnerUserIdOrderByUpdatedAtDesc(candidateId)
                application.toJobApplicationReviewItem(latestResume)
            }
    }

    @Transactional
    fun reviewApplication(
        username: String,
        jobId: UUID,
        applicationId: UUID,
        request: UpdateJobApplicationReviewRequest,
    ): JobApplicationReviewItemResponse {
        val operator = getOperator(username)
        val job = getJobEntity(jobId)
        assertCanManageJob(operator.role, job.createdBy?.username, username, "Only the job creator or an admin can review applications for this job")

        if (request.status == JobApplicationStatus.WITHDRAWN) {
            throw IllegalArgumentException("HR review actions cannot set application status to WITHDRAWN")
        }

        val application = jobApplicationRepository.findByIdAndJobId(applicationId, jobId)
            ?: throw ResourceNotFoundException("Job application not found")

        application.status = request.status
        application.reviewNote = request.reviewNote?.trim()?.ifBlank { null }
        val saved = jobApplicationRepository.save(application)
        val candidateId = requireNotNull(saved.user.id)
        val latestResume = resumeRepository.findTopByOwnerUserIdOrderByUpdatedAtDesc(candidateId)
        return saved.toJobApplicationReviewItem(latestResume)
    }

    private fun getOperator(username: String) = userRepository.findByUsername(username)
        .orElseThrow { ResourceNotFoundException("User not found") }

    private fun getJobEntity(jobId: UUID) = jobRepository.findById(jobId)
        .orElseThrow { ResourceNotFoundException("Job not found") }

    private fun assertCanManageJob(role: UserRole, ownerUsername: String?, operatorUsername: String, message: String) {
        val isAdmin = role == UserRole.ADMIN
        val isCreator = ownerUsername == operatorUsername
        if (!isAdmin && !isCreator) {
            throw AccessDeniedException(message)
        }
    }

    private fun persistJobEmbedding(job: Job, embedding: String) {
        val jobId = requireNotNull(job.id)
        if (embeddingService.shouldUseNativeVectorStorage()) {
            jobRepository.updateEmbedding(jobId, embedding)
            job.embedding = embedding
        } else {
            job.runtimeEmbedding = embedding
        }
        job.runtimeEmbedding = embedding
    }
}