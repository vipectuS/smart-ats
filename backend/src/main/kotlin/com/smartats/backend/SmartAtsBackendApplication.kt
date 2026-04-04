package com.smartats.backend

import com.smartats.backend.config.JwtProperties
import com.smartats.backend.config.EmbeddingProperties
import com.smartats.backend.config.InternalCallbackProperties
import com.smartats.backend.config.ResumeQueueProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(
    JwtProperties::class,
    ResumeQueueProperties::class,
    InternalCallbackProperties::class,
    EmbeddingProperties::class,
)
class SmartAtsBackendApplication

fun main(args: Array<String>) {
    runApplication<SmartAtsBackendApplication>(*args)
}