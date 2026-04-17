package com.smartats.backend.queue

import java.time.Instant
import java.util.UUID

data class ExternalContentReference(
    val sourceType: String,
    val url: String,
)

data class ResumeParseMessage(
    val resumeId: UUID,
    val rawContentReference: String,
    val browserPreprocessedPayload: Map<String, Any>? = null,
    val externalContentReferences: List<ExternalContentReference> = emptyList(),
    val requestedAt: Instant,
)