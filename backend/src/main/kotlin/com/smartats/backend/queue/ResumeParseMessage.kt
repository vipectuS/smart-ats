package com.smartats.backend.queue

import java.time.Instant
import java.util.UUID

data class ResumeParseMessage(
    val resumeId: UUID,
    val rawContentReference: String,
    val requestedAt: Instant,
)