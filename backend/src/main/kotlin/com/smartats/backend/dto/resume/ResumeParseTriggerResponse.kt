package com.smartats.backend.dto.resume

import java.util.UUID

data class ResumeParseTriggerResponse(
    val resumeId: UUID,
    val status: String,
    val queued: Boolean,
    val channel: String,
)