package com.smartats.backend.dto.resume

import com.smartats.backend.domain.Resume
import java.time.LocalDateTime
import java.util.UUID

data class ResumeStatusResponse(
    val resumeId: UUID,
    val status: String,
    val parsedDataAvailable: Boolean,
    val parseFailureReason: String?,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(resume: Resume): ResumeStatusResponse {
            return ResumeStatusResponse(
                resumeId = requireNotNull(resume.id),
                status = resume.status,
                parsedDataAvailable = !resume.parsedData.isNullOrEmpty(),
                parseFailureReason = resume.parseFailureReason,
                updatedAt = resume.updatedAt,
            )
        }
    }
}