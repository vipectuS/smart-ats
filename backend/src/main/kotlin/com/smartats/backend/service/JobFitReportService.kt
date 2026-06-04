package com.smartats.backend.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.smartats.backend.config.EmbeddingProperties
import com.smartats.backend.dto.xai.JobFitReportRequest
import com.smartats.backend.dto.xai.StructuredJobFitReport
import org.springframework.stereotype.Service
import java.time.Duration
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Service
class JobFitReportService(
    private val embeddingProperties: EmbeddingProperties,
    private val objectMapper: ObjectMapper,
) {

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(embeddingProperties.requestTimeoutMillis))
        .version(HttpClient.Version.HTTP_1_1)
        .build()

    fun generate(request: JobFitReportRequest): StructuredJobFitReport {
        val url = embeddingProperties.aiServiceBaseUrl.trimEnd('/') + embeddingProperties.jobFitReportPath
        return runCatching {
            val requestBody = objectMapper.writeValueAsString(request)
            val httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(embeddingProperties.requestTimeoutMillis))
                .version(HttpClient.Version.HTTP_1_1)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build()

            val response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() !in 200..299) {
                throw IllegalStateException("Job fit report service returned HTTP ${response.statusCode()}: ${response.body()}")
            }

            val responseBody = response.body().ifBlank {
                throw IllegalStateException("Job fit report service returned an empty body")
            }
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
        val missingSkillSummary = request.missingSkills.take(3).joinToString("、")
        val summary = when (request.audience) {
            "candidate" -> "当前你与岗位《${request.jobTitle}》的匹配度约为 ${request.matchScore}%。"
            "hr" -> "候选人 ${request.candidateName} 与岗位《${request.jobTitle}》的匹配度约为 ${request.matchScore}%。"
            else -> "候选人与岗位《${request.jobTitle}》的当前匹配度约为 ${request.matchScore}%。"
        }
        val narrative = if (missingSkillSummary.isNotBlank()) {
            "$summary 当前仍需重点补强 $missingSkillSummary 等关键能力。"
        } else {
            "$summary 建议继续强化可量化成果证明。"
        }
        return StructuredJobFitReport(
            headline = when (fitBand) {
                "HIGH" -> "整体匹配度较高，重点完善细节即可"
                "MEDIUM" -> "具备潜力，但仍有关键差距需要补齐"
                else -> "当前仍处于早期匹配阶段，需要定向提升"
            },
            fitBand = fitBand,
            summary = summary,
            strengths = buildList {
                if (request.matchedSkills.isNotEmpty()) add("已匹配技能：${request.matchedSkills.joinToString("、")}")
                if (request.experienceScore >= java.math.BigDecimal("70")) add("经验维度已具备较强竞争力")
                if (isEmpty()) add("语义背景已呈现一定相关性")
            },
            risks = buildList {
                if (request.missingSkills.isNotEmpty()) add("待补强技能：${request.missingSkills.joinToString("、")}")
                if (request.semanticScore < java.math.BigDecimal("50")) add("当前经历描述与目标岗位的语义贴合度仍然偏弱")
            },
            improvementSuggestions = buildList {
                if (request.missingSkills.isNotEmpty()) add("优先通过项目或认证补齐 ${request.missingSkills.take(3).joinToString("、")}")
                add("把简历中的关键经历改写为更可量化的业务成果")
            },
            nextSteps = listOf("先核对待补强技能", "把最近项目重新组织成更有业务结果的证据"),
            narrative = narrative,
        )
    }
}