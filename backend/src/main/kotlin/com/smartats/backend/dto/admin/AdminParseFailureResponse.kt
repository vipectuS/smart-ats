package com.smartats.backend.dto.admin

import com.smartats.backend.domain.AdminParseFailureReviewStatus
import java.time.LocalDateTime
import java.util.UUID

data class AdminParseFailureResponse(
    val resumeId: UUID,
    val ownerUsername: String?,
    val sourceFileName: String?,
    val rawContentReference: String,
    val parseFailureCode: String?,
    val reason: String?,
    val adminReviewNote: String?,
    val reviewStatus: AdminParseFailureReviewStatus,
    val reviewedByUsername: String?,
    val reviewedAt: LocalDateTime?,
    val updatedAt: LocalDateTime,
)