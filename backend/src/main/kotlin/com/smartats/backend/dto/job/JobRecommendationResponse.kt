package com.smartats.backend.dto.job

import com.fasterxml.jackson.annotation.JsonInclude
import com.smartats.backend.dto.talent.BasicInfo
import com.smartats.backend.dto.talent.RadarScores
import com.smartats.backend.dto.talent.Skill
import com.smartats.backend.dto.xai.StructuredJobFitReport
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class JobRecommendationResponse(
    val id: UUID,
    val jobId: UUID,
    val resumeId: UUID,
    val matchScore: BigDecimal,
    val xaiReasoning: String,
    val xaiReport: StructuredJobFitReport?,
    val candidate: JobRecommendationCandidateResponse,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class JobRecommendationCandidateResponse(
    val candidateName: String?,
    val contactInfo: String?,
    val status: String,
    val basicInfo: BasicInfo?,
    val radarScores: RadarScores?,
    val skills: List<Skill> = emptyList(),
    val parsedData: Map<String, Any>?,
)

data class JobEvaluationResponse(
    val jobId: UUID,
    val evaluatedCount: Int,
    val appliedWeights: AppliedEvaluationWeights,
    val recommendations: List<JobRecommendationResponse>,
)