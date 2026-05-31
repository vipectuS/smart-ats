package com.smartats.backend

import com.smartats.backend.config.JwtProperties
import com.smartats.backend.config.AdminSeedProperties
import com.smartats.backend.config.EmbeddingProperties
import com.smartats.backend.config.GovernanceProperties
import com.smartats.backend.config.InternalCallbackProperties
import com.smartats.backend.config.ResumeQueueProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties(
    AdminSeedProperties::class,
    JwtProperties::class,
    ResumeQueueProperties::class,
    InternalCallbackProperties::class,
    EmbeddingProperties::class,
    GovernanceProperties::class,
)
class SmartAtsBackendApplication

fun main(args: Array<String>) {
    runApplication<SmartAtsBackendApplication>(*args)
}