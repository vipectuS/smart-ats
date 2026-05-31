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
    val scoreBreakdown: JobRecommendationScoreBreakdown,
    val xaiReasoning: String,
    val xaiReport: StructuredJobFitReport?,
    val candidate: JobRecommendationCandidateResponse,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

data class JobRecommendationScoreBreakdown(
    val skillScore: BigDecimal,
    val experienceScore: BigDecimal,
    val educationScore: BigDecimal,
    val semanticScore: BigDecimal,
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
    val currentEvaluation: JobEvaluationVersionResponse? = null,
    val previousEvaluation: JobEvaluationVersionResponse? = null,
    val recommendations: List<JobRecommendationResponse>,
)

data class JobEvaluationVersionResponse(
    val evaluationId: UUID,
    val versionNumber: Int,
    val evaluatedAt: LocalDateTime,
    val evaluatedByUsername: String?,
    val evaluatedCount: Int,
    val evaluationNote: String? = null,
    val appliedWeights: AppliedEvaluationWeights,
    val comparisonToPrevious: JobEvaluationDeltaSummary? = null,
    val topRecommendations: List<JobEvaluationRecommendationSnapshot> = emptyList(),
)

data class JobEvaluationRecommendationSnapshot(
    val rank: Int,
    val resumeId: UUID,
    val candidateName: String?,
    val matchScore: BigDecimal,
)

data class JobEvaluationDeltaSummary(
    val summary: String,
    val weightChanges: List<JobEvaluationWeightDelta> = emptyList(),
    val topCandidateChange: JobEvaluationTopCandidateChange? = null,
)

data class JobEvaluationWeightDelta(
    val dimension: String,
    val label: String,
    val previousWeight: BigDecimal,
    val currentWeight: BigDecimal,
    val deltaWeight: BigDecimal,
)

data class JobEvaluationTopCandidateChange(
    val changed: Boolean,
    val previousCandidateName: String?,
    val currentCandidateName: String?,
    val previousMatchScore: BigDecimal?,
    val currentMatchScore: BigDecimal?,
    val scoreDelta: BigDecimal?,
    val enteredCandidates: List<String> = emptyList(),
    val droppedCandidates: List<String> = emptyList(),
)