package com.smartats.backend.dto.admin

data class AdminOverviewTotalsResponse(
    val totalUsers: Long,
    val totalJobs: Long,
    val totalResumes: Long,
    val totalSkillEntries: Long,
)