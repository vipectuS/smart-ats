package com.smartats.backend.dto.admin

import com.smartats.backend.domain.AdminParseFailureReviewStatus
import jakarta.validation.constraints.Size

data class AdminParseFailureReviewRequest(
    @field:Size(max = 1000, message = "Review note must be at most 1000 characters")
    val note: String? = null,
    val reviewStatus: AdminParseFailureReviewStatus? = null,
)