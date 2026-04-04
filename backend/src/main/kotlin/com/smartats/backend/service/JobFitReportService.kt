package com.smartats.backend.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.smartats.backend.config.EmbeddingProperties
import com.smartats.backend.dto.xai.JobFitReportRequest
import com.smartats.backend.dto.xai.StructuredJobFitReport
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.time.Duration

@Service
class JobFitReportService(
    private val embeddingProperties: EmbeddingProperties,
    private val objectMapper: ObjectMapper,
    restTemplateBuilder: RestTemplateBuilder,
) {

    private val restTemplate: RestTemplate = restTemplateBuilder
        .setConnectTimeout(Duration.ofMillis(embeddingProperties.requestTimeoutMillis))
        .setReadTimeout(Duration.ofMillis(embeddingProperties.requestTimeoutMillis))
        .build()

    fun generate(request: JobFitReportRequest): StructuredJobFitReport {
        val url = embeddingProperties.aiServiceBaseUrl.trimEnd('/') + embeddingProperties.jobFitReportPath
        return runCatching {
            val responseBody = restTemplate.postForObject(url, request, String::class.java)
                ?: throw IllegalStateException("Job fit report service returned an empty body")
            objectMapper.readValue(responseBody, StructuredJobFitReport::class.java)
        }.getOrElse {
            fallbackReport(request)
        }
    }

    private fun fallbackReport(request: JobFitReportRequest): StructuredJobFitReport {
        val fitBand = when {
            request.matchScore >= java.math.BigDecimal("80") -> "HIGH"
            request.matchScore >= java.math.BigDecimal("55") -> "MEDIUM"
            else -> "LOW"
        }
        val summary = when (request.audience) {
            "candidate" -> "You currently match ${request.matchScore}% of ${request.jobTitle}."
            else -> "${request.candidateName} currently matches ${request.matchScore}% of ${request.jobTitle}."
        }
        return StructuredJobFitReport(
            headline = when (fitBand) {
                "HIGH" -> "Strong fit with focused polish remaining"
                "MEDIUM" -> "Promising fit with visible gaps to close"
                else -> "Early-stage fit that needs targeted improvement"
            },
            fitBand = fitBand,
            summary = summary,
            strengths = buildList {
                if (request.matchedSkills.isNotEmpty()) add("Matched skills: ${request.matchedSkills.joinToString(", ")}")
                if (request.experienceScore >= java.math.BigDecimal("70")) add("Experience evidence is already competitive")
                if (isEmpty()) add("Semantic background still shows partial alignment")
            },
            risks = buildList {
                if (request.missingSkills.isNotEmpty()) add("Missing skills: ${request.missingSkills.joinToString(", ")}")
                if (request.semanticScore < java.math.BigDecimal("50")) add("Resume language is still weaker than the target role")
            },
            improvementSuggestions = buildList {
                if (request.missingSkills.isNotEmpty()) add("Prioritize ${request.missingSkills.take(3).joinToString(", ")} in projects or certifications")
                add("Refresh resume bullets with measurable impact for the target role")
            },
            nextSteps = listOf("Review the missing skills list", "Reframe recent projects with stronger business outcomes"),
            narrative = "$summary ${request.missingSkills.take(3).joinToString(", ").ifBlank { "Keep strengthening quantified achievements." }}".trim(),
        )
    }
}