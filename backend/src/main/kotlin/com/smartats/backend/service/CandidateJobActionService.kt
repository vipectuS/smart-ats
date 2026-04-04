package com.smartats.backend.service

import com.smartats.backend.domain.JobApplication
import com.smartats.backend.domain.JobApplicationStatus
import com.smartats.backend.domain.JobFavorite
import com.smartats.backend.domain.JobFavoriteStatus
import com.smartats.backend.domain.JobIgnore
import com.smartats.backend.domain.JobIgnoreStatus
import com.smartats.backend.domain.UserRole
import com.smartats.backend.dto.candidate.CandidateTimelineAction
import com.smartats.backend.dto.candidate.CandidateJobActionListItemResponse
import com.smartats.backend.dto.candidate.CandidateJobActionResponse
import com.smartats.backend.dto.candidate.JobActionStateResponse
import com.smartats.backend.dto.candidate.TimelineEventDTO
import com.smartats.backend.dto.candidate.toCandidateActionListItem
import com.smartats.backend.exception.ResourceNotFoundException
import com.smartats.backend.repository.JobApplicationRepository
import com.smartats.backend.repository.JobFavoriteRepository
import com.smartats.backend.repository.JobIgnoreRepository
import com.smartats.backend.repository.JobRepository
import com.smartats.backend.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneOffset
import java.time.LocalDateTime
import java.util.UUID

@Service
class CandidateJobActionService(
    private val userRepository: UserRepository,
    private val jobRepository: JobRepository,
    private val jobApplicationRepository: JobApplicationRepository,
    private val jobFavoriteRepository: JobFavoriteRepository,
    private val jobIgnoreRepository: JobIgnoreRepository,
) {

    @Transactional
    fun apply(username: String, jobId: UUID): CandidateJobActionResponse {
        val user = getCandidateUser(username)
        val job = jobRepository.findById(jobId).orElseThrow { ResourceNotFoundException("Job not found") }
        val existing = jobApplicationRepository.findByUserIdAndJobId(requireNotNull(user.id), jobId)
        if (existing != null) {
            if (existing.status == JobApplicationStatus.APPLIED) {
                return CandidateJobActionResponse(jobId, JobApplicationStatus.APPLIED.name, false, false, true, existing.createdAt, getActionState(requireNotNull(user.id), jobId))
            }

            existing.status = JobApplicationStatus.APPLIED
            val savedExisting = jobApplicationRepository.save(existing)
            return CandidateJobActionResponse(jobId, JobApplicationStatus.APPLIED.name, false, true, true, savedExisting.createdAt, getActionState(requireNotNull(user.id), jobId))
        }

        val saved = jobApplicationRepository.save(JobApplication(user = user, job = job, status = JobApplicationStatus.APPLIED))
        return CandidateJobActionResponse(jobId, JobApplicationStatus.APPLIED.name, true, true, true, saved.createdAt, getActionState(requireNotNull(user.id), jobId))
    }

    @Transactional
    fun withdraw(username: String, jobId: UUID): CandidateJobActionResponse {
        val user = getCandidateUser(username)
        val existing = jobApplicationRepository.findByUserIdAndJobId(requireNotNull(user.id), jobId)
        if (existing == null) {
            return CandidateJobActionResponse(jobId, JobApplicationStatus.WITHDRAWN.name, false, false, false, LocalDateTime.now(), getActionState(requireNotNull(user.id), jobId))
        }
        if (existing.status == JobApplicationStatus.WITHDRAWN) {
            return CandidateJobActionResponse(jobId, JobApplicationStatus.WITHDRAWN.name, false, false, false, existing.createdAt, getActionState(requireNotNull(user.id), jobId))
        }

        existing.status = JobApplicationStatus.WITHDRAWN
        val saved = jobApplicationRepository.save(existing)
        return CandidateJobActionResponse(jobId, JobApplicationStatus.WITHDRAWN.name, false, true, false, saved.createdAt, getActionState(requireNotNull(user.id), jobId))
    }

    @Transactional
    fun favorite(username: String, jobId: UUID): CandidateJobActionResponse {
        val user = getCandidateUser(username)
        val job = jobRepository.findById(jobId).orElseThrow { ResourceNotFoundException("Job not found") }
        val existing = jobFavoriteRepository.findByUserIdAndJobId(requireNotNull(user.id), jobId)
        if (existing != null) {
            if (existing.status == JobFavoriteStatus.FAVORITED) {
                return CandidateJobActionResponse(jobId, JobFavoriteStatus.FAVORITED.name, false, false, true, existing.createdAt, getActionState(requireNotNull(user.id), jobId))
            }

            existing.status = JobFavoriteStatus.FAVORITED
            val savedExisting = jobFavoriteRepository.save(existing)
            return CandidateJobActionResponse(jobId, JobFavoriteStatus.FAVORITED.name, false, true, true, savedExisting.createdAt, getActionState(requireNotNull(user.id), jobId))
        }

        val saved = jobFavoriteRepository.save(JobFavorite(user = user, job = job, status = JobFavoriteStatus.FAVORITED))
        return CandidateJobActionResponse(jobId, JobFavoriteStatus.FAVORITED.name, true, true, true, saved.createdAt, getActionState(requireNotNull(user.id), jobId))
    }

    @Transactional
    fun unfavorite(username: String, jobId: UUID): CandidateJobActionResponse {
        val user = getCandidateUser(username)
        val existing = jobFavoriteRepository.findByUserIdAndJobId(requireNotNull(user.id), jobId)
        if (existing == null) {
            return CandidateJobActionResponse(jobId, JobFavoriteStatus.UNFAVORITED.name, false, false, false, LocalDateTime.now(), getActionState(requireNotNull(user.id), jobId))
        }
        if (existing.status == JobFavoriteStatus.UNFAVORITED) {
            return CandidateJobActionResponse(jobId, JobFavoriteStatus.UNFAVORITED.name, false, false, false, existing.createdAt, getActionState(requireNotNull(user.id), jobId))
        }

        existing.status = JobFavoriteStatus.UNFAVORITED
        val saved = jobFavoriteRepository.save(existing)
        return CandidateJobActionResponse(jobId, JobFavoriteStatus.UNFAVORITED.name, false, true, false, saved.createdAt, getActionState(requireNotNull(user.id), jobId))
    }

    @Transactional
    fun ignore(username: String, jobId: UUID): CandidateJobActionResponse {
        val user = getCandidateUser(username)
        val job = jobRepository.findById(jobId).orElseThrow { ResourceNotFoundException("Job not found") }
        val existing = jobIgnoreRepository.findByUserIdAndJobId(requireNotNull(user.id), jobId)
        if (existing != null) {
            if (existing.status == JobIgnoreStatus.IGNORED) {
                return CandidateJobActionResponse(jobId, JobIgnoreStatus.IGNORED.name, false, false, true, existing.createdAt, getActionState(requireNotNull(user.id), jobId))
            }

            existing.status = JobIgnoreStatus.IGNORED
            val savedExisting = jobIgnoreRepository.save(existing)
            return CandidateJobActionResponse(jobId, JobIgnoreStatus.IGNORED.name, false, true, true, savedExisting.createdAt, getActionState(requireNotNull(user.id), jobId))
        }

        val saved = jobIgnoreRepository.save(JobIgnore(user = user, job = job, status = JobIgnoreStatus.IGNORED))
        return CandidateJobActionResponse(jobId, JobIgnoreStatus.IGNORED.name, true, true, true, saved.createdAt, getActionState(requireNotNull(user.id), jobId))
    }

    @Transactional
    fun unignore(username: String, jobId: UUID): CandidateJobActionResponse {
        val user = getCandidateUser(username)
        val existing = jobIgnoreRepository.findByUserIdAndJobId(requireNotNull(user.id), jobId)
        if (existing == null) {
            return CandidateJobActionResponse(jobId, JobIgnoreStatus.UNIGNORED.name, false, false, false, LocalDateTime.now(), getActionState(requireNotNull(user.id), jobId))
        }
        if (existing.status == JobIgnoreStatus.UNIGNORED) {
            return CandidateJobActionResponse(jobId, JobIgnoreStatus.UNIGNORED.name, false, false, false, existing.createdAt, getActionState(requireNotNull(user.id), jobId))
        }

        existing.status = JobIgnoreStatus.UNIGNORED
        val saved = jobIgnoreRepository.save(existing)
        return CandidateJobActionResponse(jobId, JobIgnoreStatus.UNIGNORED.name, false, true, false, saved.createdAt, getActionState(requireNotNull(user.id), jobId))
    }

    @Transactional(readOnly = true)
    fun listApplications(username: String): List<CandidateJobActionListItemResponse> {
        val user = getCandidateUser(username)
        val userId = requireNotNull(user.id)
        return jobApplicationRepository.findByUserIdAndStatusOrderByUpdatedAtDesc(userId, JobApplicationStatus.APPLIED)
            .map { action ->
                action.job.toCandidateActionListItem(JobApplicationStatus.APPLIED.name, action.createdAt, getActionState(userId, requireNotNull(action.job.id)))
            }
    }

    @Transactional(readOnly = true)
    fun listFavorites(username: String): List<CandidateJobActionListItemResponse> {
        val user = getCandidateUser(username)
        val userId = requireNotNull(user.id)
        return jobFavoriteRepository.findByUserIdAndStatusOrderByUpdatedAtDesc(userId, JobFavoriteStatus.FAVORITED)
            .map { action ->
                action.job.toCandidateActionListItem(JobFavoriteStatus.FAVORITED.name, action.createdAt, getActionState(userId, requireNotNull(action.job.id)))
            }
    }

    @Transactional(readOnly = true)
    fun listIgnores(username: String): List<CandidateJobActionListItemResponse> {
        val user = getCandidateUser(username)
        val userId = requireNotNull(user.id)
        return jobIgnoreRepository.findByUserIdAndStatusOrderByUpdatedAtDesc(userId, JobIgnoreStatus.IGNORED)
            .map { action ->
                action.job.toCandidateActionListItem(JobIgnoreStatus.IGNORED.name, action.createdAt, getActionState(userId, requireNotNull(action.job.id)))
            }
    }

    @Transactional(readOnly = true)
    fun listTimeline(username: String): List<TimelineEventDTO> {
        val user = getCandidateUser(username)
        val userId = requireNotNull(user.id)

        val applicationEvents = jobApplicationRepository.findByUserIdOrderByUpdatedAtDesc(userId)
            .map { action ->
                TimelineEventDTO(
                    action = action.status.toTimelineAction(),
                    jobTitle = action.job.title,
                    companyName = action.job.createdBy?.username ?: "Unknown",
                    timestamp = action.updatedAt.toInstant(ZoneOffset.UTC),
                )
            }
        val favoriteEvents = jobFavoriteRepository.findByUserIdOrderByUpdatedAtDesc(userId)
            .map { action ->
                TimelineEventDTO(
                    action = action.status.toTimelineAction(),
                    jobTitle = action.job.title,
                    companyName = action.job.createdBy?.username ?: "Unknown",
                    timestamp = action.updatedAt.toInstant(ZoneOffset.UTC),
                )
            }
        val ignoreEvents = jobIgnoreRepository.findByUserIdOrderByUpdatedAtDesc(userId)
            .map { action ->
                TimelineEventDTO(
                    action = action.status.toTimelineAction(),
                    jobTitle = action.job.title,
                    companyName = action.job.createdBy?.username ?: "Unknown",
                    timestamp = action.updatedAt.toInstant(ZoneOffset.UTC),
                )
            }

        return (applicationEvents + favoriteEvents + ignoreEvents)
            .sortedByDescending { it.timestamp }
    }

    @Transactional(readOnly = true)
    fun getActionState(userId: UUID, jobId: UUID): JobActionStateResponse {
        return JobActionStateResponse(
            applied = jobApplicationRepository.existsByUserIdAndJobIdAndStatus(userId, jobId, JobApplicationStatus.APPLIED),
            favorited = jobFavoriteRepository.existsByUserIdAndJobIdAndStatus(userId, jobId, JobFavoriteStatus.FAVORITED),
            ignored = jobIgnoreRepository.existsByUserIdAndJobIdAndStatus(userId, jobId, JobIgnoreStatus.IGNORED),
        )
    }

    @Transactional(readOnly = true)
    fun getActionStateMap(userId: UUID, jobIds: Collection<UUID>): Map<UUID, JobActionStateResponse> {
        return jobIds.associateWith { jobId -> getActionState(userId, jobId) }
    }

    @Transactional(readOnly = true)
    fun getIgnoredJobIds(userId: UUID): Set<UUID> {
        return jobIgnoreRepository.findByUserIdAndStatusOrderByUpdatedAtDesc(userId, JobIgnoreStatus.IGNORED)
            .mapNotNull { it.job.id }
            .toSet()
    }

    private fun getCandidateUser(username: String) = userRepository.findByUsername(username)
        .orElseThrow { ResourceNotFoundException("User not found") }
        .also { user ->
            if (user.role != UserRole.CANDIDATE) {
                throw ResourceNotFoundException("Candidate user not found")
            }
        }

    private fun JobApplicationStatus.toTimelineAction(): CandidateTimelineAction {
        return when (this) {
            JobApplicationStatus.APPLIED -> CandidateTimelineAction.APPLIED
            JobApplicationStatus.WITHDRAWN -> CandidateTimelineAction.WITHDRAWN
        }
    }

    private fun JobFavoriteStatus.toTimelineAction(): CandidateTimelineAction {
        return when (this) {
            JobFavoriteStatus.FAVORITED -> CandidateTimelineAction.FAVORITED
            JobFavoriteStatus.UNFAVORITED -> CandidateTimelineAction.UNFAVORITED
        }
    }

    private fun JobIgnoreStatus.toTimelineAction(): CandidateTimelineAction {
        return when (this) {
            JobIgnoreStatus.IGNORED -> CandidateTimelineAction.IGNORED
            JobIgnoreStatus.UNIGNORED -> CandidateTimelineAction.UNIGNORED
        }
    }
}