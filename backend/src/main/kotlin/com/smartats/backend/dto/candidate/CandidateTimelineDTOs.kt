package com.smartats.backend.dto.candidate

import java.time.Instant

enum class CandidateTimelineAction {
    APPLIED,
    INTERVIEW,
    REJECTED,
    WITHDRAWN,
    FAVORITED,
    UNFAVORITED,
    IGNORED,
    UNIGNORED,
}

data class TimelineEventDTO(
    val action: CandidateTimelineAction,
    val jobTitle: String,
    val companyName: String,
    val timestamp: Instant,
)