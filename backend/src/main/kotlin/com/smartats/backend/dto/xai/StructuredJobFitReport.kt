package com.smartats.backend.dto.xai

import java.math.BigDecimal

data class StructuredJobFitReport(
    val headline: String,
    val fitBand: String,
    val summary: String,
    val strengths: List<String>,
    val risks: List<String>,
    val improvementSuggestions: List<String>,
    val nextSteps: List<String>,
    val narrative: String,
)

data class JobFitReportRequest(
    val audience: String,
    val candidateName: String,
    val jobTitle: String,
    val jobDescription: String,
    val jobRequirements: Map<String, Any>?,
    val matchScore: BigDecimal,
    val semanticScore: BigDecimal,
    val skillScore: BigDecimal,
    val experienceScore: BigDecimal,
    val educationScore: BigDecimal,
    val matchedSkills: List<String>,
    val missingSkills: List<String>,
)