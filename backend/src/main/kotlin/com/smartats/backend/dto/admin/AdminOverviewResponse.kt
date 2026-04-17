package com.smartats.backend.dto.admin

data class AdminOverviewResponse(
    val totals: AdminOverviewTotalsResponse,
    val usersByRole: List<AdminDistributionItemResponse>,
    val resumesByStatus: List<AdminDistributionItemResponse>,
    val latestParseFailures: List<AdminParseFailureResponse>,
)