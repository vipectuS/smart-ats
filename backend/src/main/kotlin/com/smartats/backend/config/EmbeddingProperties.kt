package com.smartats.backend.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.embedding")
data class EmbeddingProperties(
    val dimension: Int = 1536,
    val aiServiceBaseUrl: String = "http://127.0.0.1:8000",
    val embeddingPath: String = "/api/embeddings",
    val jobFitReportPath: String = "/api/job-fit-report",
    val requestTimeoutMillis: Long = 5000,
    val nativeVectorStorageEnabled: Boolean = true,
    val pgvectorQueryEnabled: Boolean = true,
)