package com.smartats.backend.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.seed-default-admin")
data class AdminSeedProperties(
    val enabled: Boolean = false,
    val username: String = "admin",
    val password: String = "change-me-admin-password",
    val email: String = "admin@example.com",
)
