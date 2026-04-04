package com.smartats.backend.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.resume-queue")
data class ResumeQueueProperties(
    val channel: String,
    val listenerEnabled: Boolean,
    val mockProcessingDelayMillis: Long,
)