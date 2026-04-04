package com.smartats.backend.dto.resume

import com.fasterxml.jackson.annotation.JsonInclude
import com.smartats.backend.domain.Resume
import java.time.LocalDateTime
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ResumeResponse(
    val id: UUID,
    val candidateName: String?,
    val contactInfo: String?,
    val rawContentReference: String,
    val parsedData: Map<String, Any>?,
    val parseFailureReason: String?,
    val status: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(resume: Resume): ResumeResponse {
            return ResumeResponse(
                id = requireNotNull(resume.id),
                candidateName = resume.candidateName,
                contactInfo = resume.contactInfo,
                rawContentReference = resume.rawContentReference,
                parsedData = resume.parsedData,
                parseFailureReason = resume.parseFailureReason,
                status = resume.status,
                createdAt = resume.createdAt,
                updatedAt = resume.updatedAt,
            )
        }
    }
}