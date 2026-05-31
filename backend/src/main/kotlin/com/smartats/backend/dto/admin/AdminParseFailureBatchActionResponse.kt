package com.smartats.backend.dto.admin

import com.smartats.backend.domain.AdminParseFailureReviewStatus
import java.util.UUID

data class AdminParseFailureBatchActionResponse(
    val processedCount: Int,
    val resumeIds: List<UUID>,
    val reviewStatus: AdminParseFailureReviewStatus?,
    val queued: Boolean,
)