package com.smartats.backend.dto.admin

import java.time.LocalDateTime
import java.util.UUID

data class AdminParseFailureResponse(
    val resumeId: UUID,
    val ownerUsername: String?,
    val sourceFileName: String?,
    val rawContentReference: String,
    val reason: String?,
    val updatedAt: LocalDateTime,
)