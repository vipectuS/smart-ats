package com.smartats.backend.dto.candidate

import com.fasterxml.jackson.annotation.JsonInclude
import com.smartats.backend.domain.CandidateProfile
import com.smartats.backend.domain.Resume
import com.smartats.backend.domain.User
import com.smartats.backend.dto.resume.parseFailureMetadata
import java.time.LocalDateTime
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CandidateProfileResponse(
    val userId: UUID,
    val username: String,
    val email: String,
    val githubUrl: String?,
    val portfolioUrl: String?,
    val latestResume: CandidateResumeSummaryResponse?,
    val updatedAt: LocalDateTime?,
) {
    companion object {
        fun from(user: User, profile: CandidateProfile?, latestResume: Resume?): CandidateProfileResponse {
            return CandidateProfileResponse(
                userId = requireNotNull(user.id),
                username = user.username,
                email = user.email,
                githubUrl = profile?.githubUrl,
                portfolioUrl = profile?.portfolioUrl,
                latestResume = latestResume?.let { CandidateResumeSummaryResponse.from(it) },
                updatedAt = profile?.updatedAt,
            )
        }
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CandidateResumeSummaryResponse(
    val resumeId: UUID,
    val candidateName: String?,
    val contactInfo: String?,
    val rawContentReference: String,
    val status: String,
    val parseFailureCode: String?,
    val parseFailureReason: String?,
    val parsedData: Map<String, Any>?,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(resume: Resume): CandidateResumeSummaryResponse {
            val parseFailure = parseFailureMetadata(resume.parseFailureReason)
            return CandidateResumeSummaryResponse(
                resumeId = requireNotNull(resume.id),
                candidateName = resume.candidateName,
                contactInfo = resume.contactInfo,
                rawContentReference = resume.rawContentReference,
                status = resume.status,
                parseFailureCode = parseFailure.code,
                parseFailureReason = parseFailure.reason,
                parsedData = resume.parsedData,
                updatedAt = resume.updatedAt,
            )
        }
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CandidateResumeDetailResponse(
    val resumeId: UUID,
    val candidateName: String?,
    val contactInfo: String?,
    val rawContentReference: String,
    val status: String,
    val parseFailureCode: String?,
    val parseFailureReason: String?,
    val parsedData: Map<String, Any>?,
    val browserPreprocessedPayload: Map<String, Any>?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(resume: Resume): CandidateResumeDetailResponse {
            val parseFailure = parseFailureMetadata(resume.parseFailureReason)
            return CandidateResumeDetailResponse(
                resumeId = requireNotNull(resume.id),
                candidateName = resume.candidateName,
                contactInfo = resume.contactInfo,
                rawContentReference = resume.rawContentReference,
                status = resume.status,
                parseFailureCode = parseFailure.code,
                parseFailureReason = parseFailure.reason,
                parsedData = resume.parsedData,
                browserPreprocessedPayload = resume.browserPreprocessedPayload,
                createdAt = resume.createdAt,
                updatedAt = resume.updatedAt,
            )
        }
    }
}