package com.smartats.backend.dto.candidate

import com.smartats.backend.domain.Job
import java.time.LocalDateTime
import java.util.UUID

data class JobActionStateResponse(
    val applied: Boolean,
    val favorited: Boolean,
    val ignored: Boolean,
    val applicationStatus: String? = null,
)

data class CandidateJobActionResponse(
    val jobId: UUID,
    val actionType: String,
    val created: Boolean,
    val changed: Boolean,
    val active: Boolean,
    val createdAt: LocalDateTime,
    val actionState: JobActionStateResponse,
)

data class CandidateJobActionListItemResponse(
    val jobId: UUID,
    val title: String,
    val description: String,
    val requirements: Map<String, Any>?,
    val actionType: String,
    val actionCreatedAt: LocalDateTime,
    val actionUpdatedAt: LocalDateTime,
    val actionState: JobActionStateResponse,
)

fun Job.toCandidateActionListItem(
    actionType: String,
    actionCreatedAt: LocalDateTime,
    actionUpdatedAt: LocalDateTime,
    actionState: JobActionStateResponse,
): CandidateJobActionListItemResponse {
    return CandidateJobActionListItemResponse(
        jobId = requireNotNull(id),
        title = title,
        description = description,
        requirements = requirements,
        actionType = actionType,
        actionCreatedAt = actionCreatedAt,
        actionUpdatedAt = actionUpdatedAt,
        actionState = actionState,
    )
}