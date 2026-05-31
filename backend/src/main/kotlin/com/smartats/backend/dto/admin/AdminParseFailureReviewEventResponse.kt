package com.smartats.backend.dto.admin

import com.smartats.backend.domain.AdminParseFailureReviewActionType
import com.smartats.backend.domain.AdminParseFailureReviewEvent
import com.smartats.backend.domain.AdminParseFailureReviewStatus
import java.time.LocalDateTime
import java.util.UUID

data class AdminParseFailureReviewEventResponse(
    val id: UUID,
    val resumeId: UUID,
    val adminUsername: String,
    val actionType: AdminParseFailureReviewActionType,
    val note: String?,
    val previousReviewStatus: AdminParseFailureReviewStatus,
    val nextReviewStatus: AdminParseFailureReviewStatus,
    val resumeStatusAfterAction: String,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(event: AdminParseFailureReviewEvent): AdminParseFailureReviewEventResponse {
            return AdminParseFailureReviewEventResponse(
                id = requireNotNull(event.id),
                resumeId = requireNotNull(event.resume.id),
                adminUsername = event.adminUsername,
                actionType = event.actionType,
                note = event.note,
                previousReviewStatus = event.previousReviewStatus,
                nextReviewStatus = event.nextReviewStatus,
                resumeStatusAfterAction = event.resumeStatusAfterAction,
                createdAt = event.createdAt,
            )
        }
    }
}