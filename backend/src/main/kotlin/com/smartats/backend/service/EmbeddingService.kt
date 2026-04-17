package com.smartats.backend.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.smartats.backend.config.EmbeddingProperties
import com.smartats.backend.domain.Job
import com.smartats.backend.dto.talent.TalentProfile
import org.springframework.stereotype.Service
import java.time.Duration
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.math.sqrt

@Service
class EmbeddingService(
    private val embeddingProperties: EmbeddingProperties,
    private val objectMapper: ObjectMapper,
) {

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(embeddingProperties.requestTimeoutMillis))
        .version(HttpClient.Version.HTTP_1_1)
        .build()

    fun generateJobEmbedding(job: Job): String {
        return generateEmbedding(buildJobText(job))
    }

    fun generateResumeEmbedding(parsedData: Map<String, Any>): String {
        val embeddingText = runCatching {
            val profile = objectMapper.convertValue(parsedData, TalentProfile::class.java)
            buildResumeText(profile)
        }.getOrElse {
            objectMapper.writeValueAsString(parsedData)
        }

        return generateEmbedding(embeddingText)
    }

    fun shouldUseNativeVectorStorage(): Boolean = embeddingProperties.nativeVectorStorageEnabled

    fun shouldUsePgvectorQuery(): Boolean = embeddingProperties.pgvectorQueryEnabled

    fun cosineSimilarity(leftLiteral: String, rightLiteral: String): Double {
        val left = parseLiteral(leftLiteral)
        val right = parseLiteral(rightLiteral)
        if (left.isEmpty() || right.isEmpty()) {
            return 0.0
        }

        val size = minOf(left.size, right.size)
        var dot = 0.0
        var leftNorm = 0.0
        var rightNorm = 0.0
        for (index in 0 until size) {
            dot += left[index] * right[index]
            leftNorm += left[index] * left[index]
            rightNorm += right[index] * right[index]
        }

        if (leftNorm == 0.0 || rightNorm == 0.0) {
            return 0.0
        }

        return (dot / (sqrt(leftNorm) * sqrt(rightNorm))).coerceIn(0.0, 1.0)
    }

    private fun generateEmbedding(text: String): String {
        return requestRemoteEmbedding(text)
    }

    private fun requestRemoteEmbedding(text: String): String {
        val url = embeddingProperties.aiServiceBaseUrl.trimEnd('/') + embeddingProperties.embeddingPath
        val requestBody = objectMapper.writeValueAsString(mapOf("text" to text))
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofMillis(embeddingProperties.requestTimeoutMillis))
            .version(HttpClient.Version.HTTP_1_1)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()

        val responseBody = try {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() !in 200..299) {
                throw IllegalStateException("Embedding service returned HTTP ${response.statusCode()}: ${response.body()}")
            }
            response.body().ifBlank { throw IllegalStateException("Embedding service returned an empty body") }
        } catch (exception: Exception) {
            val reason = exception.cause?.message ?: exception.message ?: "unknown remote error"
            throw IllegalStateException("Embedding service request failed: $reason", exception)
        }

        val root = objectMapper.readTree(responseBody)
        val vectorNode = extractVectorNode(root)
            ?: throw IllegalStateException("Embedding response missing vector payload")
        val dimensions = root.path("dimensions").asInt(vectorNode.size())
        if (dimensions != embeddingProperties.dimension || vectorNode.size() != embeddingProperties.dimension) {
            throw IllegalStateException(
                "Embedding dimension mismatch: expected ${embeddingProperties.dimension}, got response dimensions=$dimensions and vector size=${vectorNode.size()}"
            )
        }
        return nodeToLiteral(vectorNode)
    }

    private fun extractVectorNode(root: JsonNode): JsonNode? {
        return when {
            root.isArray -> root
            root.has("embedding") -> root.get("embedding")
            root.has("data") && root.get("data").has("embedding") -> root.get("data").get("embedding")
            else -> null
        }
    }

    private fun nodeToLiteral(node: JsonNode): String {
        val values = node.map { it.asDouble() }
        return toLiteral(values)
    }

    private fun toLiteral(values: List<Double>): String {
        return values.joinToString(prefix = "[", postfix = "]") { value ->
            String.format(java.util.Locale.US, "%.6f", value)
        }
    }

    private fun parseLiteral(literal: String): List<Double> {
        return literal.removePrefix("[")
            .removeSuffix("]")
            .split(',')
            .mapNotNull { token -> token.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull() }
    }

    private fun buildJobText(job: Job): String {
        val requirementText = job.requirements
            ?.entries
            ?.joinToString(separator = " ") { (key, value) -> "$key $value" }
            .orEmpty()

        return listOf(job.title, job.description, requirementText)
            .joinToString(separator = " ")
            .trim()
    }

    private fun buildResumeText(profile: TalentProfile): String {
        val workText = profile.workExperiences.joinToString(separator = " ") { experience ->
            listOf(
                experience.company,
                experience.title,
                experience.responsibilities.joinToString(" "),
                experience.achievements.joinToString(" "),
            ).joinToString(" ")
        }
        val educationText = profile.educationExperiences.joinToString(separator = " ") { education ->
            listOf(education.school, education.degree, education.fieldOfStudy.orEmpty()).joinToString(" ")
        }
        val skillText = profile.skills.joinToString(separator = " ") { skill ->
            listOf(skill.name, skill.category.orEmpty(), skill.proficiency.orEmpty(), skill.evidence.orEmpty())
                .joinToString(" ")
        }

        return listOf(
            profile.basicInfo.fullName,
            profile.basicInfo.headline.orEmpty(),
            profile.basicInfo.summary.orEmpty(),
            workText,
            educationText,
            skillText,
            profile.xaiReasoning,
        ).joinToString(separator = " ").trim()
    }

    private fun tokenize(text: String): List<String> {
        return text.lowercase()
            .split(Regex("[^a-z0-9+#.]+"))
            .filter { it.length >= 2 }
    }
}