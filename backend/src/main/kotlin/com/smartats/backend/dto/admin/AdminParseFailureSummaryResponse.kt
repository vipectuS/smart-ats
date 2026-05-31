package com.smartats.backend.dto.admin

import java.time.LocalDateTime

data class AdminParseFailureSummaryResponse(
    val totalFailures: Long,
    val firstFailureAt: LocalDateTime?,
    val lastFailureAt: LocalDateTime?,
    val reviewStatusCounts: List<AdminDistributionItemResponse>,
    val failureCodeCounts: List<AdminDistributionItemResponse>,
)