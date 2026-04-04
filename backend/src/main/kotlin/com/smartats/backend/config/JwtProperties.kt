package com.smartats.backend.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.security.jwt")
data class JwtProperties(
    val secret: String,
    val expirationMinutes: Long,
)