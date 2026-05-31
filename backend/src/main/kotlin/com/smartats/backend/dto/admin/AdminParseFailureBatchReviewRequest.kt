package com.smartats.backend.dto.admin

import com.smartats.backend.domain.AdminParseFailureReviewStatus
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.util.UUID

data class AdminParseFailureBatchReviewRequest(
    @field:NotEmpty(message = "resumeIds must not be empty")
    @field:Size(max = 50, message = "resumeIds must contain at most 50 items")
    val resumeIds: List<UUID>,
    @field:Size(max = 1000, message = "Review note must be at most 1000 characters")
    val note: String? = null,
    val reviewStatus: AdminParseFailureReviewStatus? = null,
)