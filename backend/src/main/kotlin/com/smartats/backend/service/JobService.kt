package com.smartats.backend.service

import com.smartats.backend.domain.Job
import com.smartats.backend.domain.JobApplicationStatus
import com.smartats.backend.domain.User
import com.smartats.backend.domain.UserRole
import com.smartats.backend.dto.PageResponse
import com.smartats.backend.dto.job.CreateJobRequest
import com.smartats.backend.dto.job.JobApplicationReviewItemResponse
import com.smartats.backend.dto.job.JobResponse
import com.smartats.backend.dto.job.UpdateJobApplicationReviewRequest
import com.smartats.backend.dto.job.UpdateJobRequest
import com.smartats.backend.dto.job.toJobApplicationReviewItem
import com.smartats.backend.exception.BadRequestException
import com.smartats.backend.exception.ResourceNotFoundException
import com.smartats.backend.repository.JobApplicationRepository
import com.smartats.backend.repository.JobRepository
import com.smartats.backend.repository.ResumeRepository
import com.smartats.backend.repository.SkillDictionaryRepository
import com.smartats.backend.repository.UserRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.UUID

@Service
class JobService(
    private val jobRepository: JobRepository,
    private val userRepository: UserRepository,
    private val jobApplicationRepository: JobApplicationRepository,
    private val resumeRepository: ResumeRepository,
    private val skillDictionaryRepository: SkillDictionaryRepository,
    private val embeddingService: EmbeddingService,
    private val recommendationRefreshTrigger: RecommendationRefreshTrigger,
    private val organizationService: OrganizationService,
) {

    @Transactional
    fun createJob(username: String, request: CreateJobRequest): JobResponse {
        val creator = getOperator(username)
        val organization = resolvePostingOrganization(creator)
        validateRequirements(request.requirements)

        val job = Job(
            title = request.title.trim(),
            description = request.description.trim(),
            requirements = request.requirements,
            createdBy = creator,
            organization = organization,
        )

        val savedJob = jobRepository.save(job)
        val embedding = embeddingService.generateJobEmbedding(savedJob)
        persistJobEmbedding(savedJob, embedding)
        scheduleRecommendationRefresh(requireNotNull(savedJob.id))
        return JobResponse.from(savedJob)
    }

    @Transactional
    fun updateJob(username: String, jobId: UUID, request: UpdateJobRequest): JobResponse {
        val operator = getOperator(username)
        val job = getJobEntity(jobId)
        assertCanManageJob(operator, job, "Only HRs from the same organization or an admin can update this job")
        validateRequirements(request.requirements)

        job.title = request.title.trim()
        job.description = request.description.trim()
        job.requirements = request.requirements

        val savedJob = jobRepository.save(job)
        val embedding = embeddingService.generateJobEmbedding(savedJob)
        persistJobEmbedding(savedJob, embedding)
        scheduleRecommendationRefresh(requireNotNull(savedJob.id))
        return JobResponse.from(savedJob)
    }

    @Transactional(readOnly = true)
    fun getJob(username: String, jobId: UUID): JobResponse {
        val operator = getOperator(username)
        val job = getJobEntity(jobId)
        assertCanViewJob(operator, job)
        return JobResponse.from(job)
    }

    @Transactional(readOnly = true)
    fun listJobs(username: String, page: Int, size: Int): PageResponse<JobResponse> {
        val operator = getOperator(username)
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val jobs = when (operator.role) {
            UserRole.HR -> {
                val organizationId = requireNotNull(organizationService.resolveHrOrganization(operator).id)
                jobRepository.findByOrganizationId(organizationId, pageable).map(JobResponse::from)
            }
            else -> jobRepository.findAll(pageable).map(JobResponse::from)
        }
        return PageResponse.from(jobs)
    }

    @Transactional(readOnly = true)
    fun listActiveApplications(username: String, jobId: UUID): List<JobApplicationReviewItemResponse> {
        val operator = getOperator(username)
        val job = getJobEntity(jobId)
        assertCanManageJob(operator, job, "Only HRs from the same organization or an admin can review applications for this job")

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
        assertCanManageJob(operator, job, "Only HRs from the same organization or an admin can review applications for this job")

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

    private fun assertCanViewJob(operator: User, job: Job) {
        if (operator.role == UserRole.ADMIN || operator.role == UserRole.CANDIDATE) {
            return
        }
        val organizationId = organizationService.resolveHrOrganization(operator).id
        if (organizationId != job.organization.id) {
            throw AccessDeniedException("HR can only access jobs within the same organization")
        }
    }

    private fun assertCanManageJob(operator: User, job: Job, message: String) {
        if (operator.role == UserRole.ADMIN) {
            return
        }
        if (operator.role != UserRole.HR) {
            throw AccessDeniedException(message)
        }
        val organizationId = organizationService.resolveHrOrganization(operator).id
        if (organizationId != job.organization.id) {
            throw AccessDeniedException(message)
        }
    }

    private fun resolvePostingOrganization(user: User) = when (user.role) {
        UserRole.HR -> organizationService.resolveHrOrganization(user)
        UserRole.ADMIN -> user.organization ?: throw BadRequestException("Admin must belong to an organization before posting jobs")
        else -> throw BadRequestException("Only HR users can create jobs")
    }

    private fun validateRequirements(requirements: Map<String, Any>) {
        val rawSkills = requirements["skills"] ?: return

        if (rawSkills !is List<*>) {
            throw BadRequestException("Requirements.skills must be an array of existing skill names")
        }

        val enabledSkillsByLowercase = skillDictionaryRepository.findEnabledNamesOrderByNameAsc()
            .associateBy { it.trim().lowercase() }
        val invalidSkills = mutableListOf<String>()

        rawSkills.forEach { entry ->
            val skillName = (entry as? String)?.trim()
            if (skillName.isNullOrEmpty()) {
                return@forEach
            }

            if (enabledSkillsByLowercase[skillName.lowercase()] == null) {
                invalidSkills += skillName
            }
        }

        if (invalidSkills.isNotEmpty()) {
            throw BadRequestException("These skills are not in the enabled skill dictionary: ${invalidSkills.joinToString(", ")}")
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

    private fun scheduleRecommendationRefresh(jobId: UUID) {
        runAfterCommit {
            recommendationRefreshTrigger.refreshJobRecommendations(jobId)
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
}