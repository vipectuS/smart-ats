package com.smartats.backend.dto.job

import com.smartats.backend.domain.JobApplication
import com.smartats.backend.domain.Resume
import java.time.LocalDateTime
import java.util.UUID

data class JobApplicantCandidateSummary(
    val id: UUID,
    val username: String,
    val email: String,
    val displayName: String,
)

data class JobApplicantResumeSummary(
    val resumeId: UUID,
    val candidateName: String?,
    val contactInfo: String?,
    val status: String,
    val updatedAt: LocalDateTime,
)

data class JobApplicationReviewItemResponse(
    val applicationId: UUID,
    val status: String,
    val appliedAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val reviewNote: String?,
    val candidate: JobApplicantCandidateSummary,
    val latestResume: JobApplicantResumeSummary?,
)

fun JobApplication.toJobApplicationReviewItem(latestResume: Resume?): JobApplicationReviewItemResponse {
    val candidate = user
    return JobApplicationReviewItemResponse(
        applicationId = requireNotNull(id),
        status = status.name,
        appliedAt = createdAt,
        updatedAt = updatedAt,
        reviewNote = reviewNote,
        candidate = JobApplicantCandidateSummary(
            id = requireNotNull(candidate.id),
            username = candidate.username,
            email = candidate.email,
            displayName = latestResume?.candidateName ?: candidate.username,
        ),
        latestResume = latestResume?.let {
            JobApplicantResumeSummary(
                resumeId = requireNotNull(it.id),
                candidateName = it.candidateName,
                contactInfo = it.contactInfo,
                status = it.status,
                updatedAt = it.updatedAt,
            )
        },
    )
}