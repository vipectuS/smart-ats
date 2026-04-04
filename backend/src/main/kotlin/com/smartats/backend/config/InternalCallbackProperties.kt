package com.smartats.backend.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.internal-callback")
data class InternalCallbackProperties(
    val headerName: String,
    val apiKey: String,
)