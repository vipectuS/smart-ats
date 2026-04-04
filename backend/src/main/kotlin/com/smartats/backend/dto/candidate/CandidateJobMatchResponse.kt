package com.smartats.backend.dto.candidate

import com.smartats.backend.dto.xai.StructuredJobFitReport
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class CandidateJobMatchResponse(
    val candidateUserId: UUID,
    val evaluatedCount: Int,
    val recommendations: List<CandidateJobRecommendationResponse>,
)

data class CandidateJobRecommendationResponse(
    val jobId: UUID,
    val title: String,
    val description: String,
    val requirements: Map<String, Any>?,
    val matchScore: BigDecimal,
    val semanticScore: BigDecimal,
    val suitabilityReport: String,
    val xaiReport: StructuredJobFitReport,
    val matchedSkills: List<String>,
    val missingSkills: List<String>,
    val actionState: JobActionStateResponse,
    val createdAt: LocalDateTime,
)