package com.smartats.backend.service

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class RecommendationRefreshTrigger(
    private val recommendationService: RecommendationService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Async
    fun refreshJobRecommendations(jobId: UUID) {
        runCatching {
            recommendationService.generateRecommendationsForJob(jobId, null)
        }.onFailure { error ->
            logger.warn("Failed to asynchronously refresh recommendations for job {}", jobId, error)
        }
    }

    @Async
    fun refreshResumeRecommendations(resumeId: UUID) {
        runCatching {
            recommendationService.refreshRecommendationsForResume(resumeId)
        }.onFailure { error ->
            logger.warn("Failed to asynchronously refresh recommendations for resume {}", resumeId, error)
        }
    }

    @Async
    fun clearResumeRecommendations(resumeId: UUID) {
        runCatching {
            recommendationService.deleteRecommendationsForResume(resumeId)
        }.onFailure { error ->
            logger.warn("Failed to asynchronously clear recommendations for resume {}", resumeId, error)
        }
    }
}