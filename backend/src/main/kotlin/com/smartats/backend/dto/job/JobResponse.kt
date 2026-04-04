package com.smartats.backend.dto.job

import com.fasterxml.jackson.annotation.JsonInclude
import com.smartats.backend.domain.Job
import java.time.LocalDateTime
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class JobResponse(
    val id: UUID,
    val title: String,
    val description: String,
    val requirements: Map<String, Any>?,
    val createdBy: JobUserSummary?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(job: Job): JobResponse {
            return JobResponse(
                id = requireNotNull(job.id),
                title = job.title,
                description = job.description,
                requirements = job.requirements,
                createdBy = job.createdBy?.let {
                    JobUserSummary(
                        id = requireNotNull(it.id),
                        username = it.username,
                        role = it.role.name,
                    )
                },
                createdAt = job.createdAt,
                updatedAt = job.updatedAt,
            )
        }
    }
}

data class JobUserSummary(
    val id: UUID,
    val username: String,
    val role: String,
)