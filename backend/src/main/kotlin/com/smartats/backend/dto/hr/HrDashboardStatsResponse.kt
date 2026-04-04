package com.smartats.backend.dto.hr

data class HrDashboardStatsResponse(
    val keyMetrics: HrDashboardKeyMetrics,
    val funnel: List<HrDashboardFunnelItem>,
    val skillsDistribution: List<HrDashboardSkillDistributionItem>,
    val trends: HrDashboardTrends,
)

data class HrDashboardKeyMetrics(
    val totalResumes: Long,
    val parsedResumes: Long,
    val interviewCount: Long,
    val offersSent: Long,
)

data class HrDashboardFunnelItem(
    val name: String,
    val value: Long,
)

data class HrDashboardSkillDistributionItem(
    val name: String,
    val value: Long,
)

data class HrDashboardTrends(
    val dates: List<String>,
    val received: List<Long>,
    val parsed: List<Long>,
)