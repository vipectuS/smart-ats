package com.smartats.backend.dto.admin

import java.time.LocalDateTime

data class AccessAuditSummaryResponse(
    val totalEvents: Long,
    val firstEventAt: LocalDateTime?,
    val lastEventAt: LocalDateTime?,
    val actionCounts: List<AdminDistributionItemResponse>,
    val actorRoleCounts: List<AdminDistributionItemResponse>,
    val targetTypeCounts: List<AdminDistributionItemResponse>,
    val sensitiveFieldCounts: List<AdminDistributionItemResponse>,
)