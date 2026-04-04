package com.smartats.backend

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sun.net.httpserver.HttpServer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.io.OutputStream
import java.net.InetSocketAddress
import kotlin.math.sqrt

@Configuration
@Profile("test")
class TestEmbeddingStubConfig {

    @Bean
    fun embeddingStubServer(): StoppableEmbeddingServer {
        return SharedEmbeddingStubServer.instance
    }

    private fun buildVector(text: String): List<Double> {
        val dimension = 1536
        val values = DoubleArray(dimension)
        val tokens = text.lowercase().split(Regex("[^a-z0-9+#.]+"))
            .filter { it.isNotBlank() }
            .ifEmpty { listOf("smartats") }

        tokens.forEachIndexed { tokenIndex, token ->
            token.forEachIndexed { charIndex, char ->
                val position = (tokenIndex * 31 + charIndex) % dimension
                values[position] += (char.code % 97) / 97.0
            }
        }

        val norm = sqrt(values.sumOf { it * it }).takeIf { it > 0.0 } ?: 1.0
        return values.map { (it / norm) }
    }
}

class StoppableEmbeddingServer(
    private val server: HttpServer,
) {
    fun stop() {
        // Keep the shared stub alive for the full JVM test run to avoid rebinding conflicts.
    }
}

private object SharedEmbeddingStubServer {
    private val mapper = jacksonObjectMapper()

    val instance: StoppableEmbeddingServer by lazy {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 18081), 0)
        server.createContext("/api/embeddings") { exchange ->
            val body = exchange.requestBody.bufferedReader().use { it.readText() }
            val text = mapper.readTree(body).path("text").asText("")
            val vector = buildVector(text)
            val payload = mapper.writeValueAsBytes(mapOf("embedding" to vector, "dimensions" to vector.size))
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, payload.size.toLong())
            exchange.responseBody.use { responseBody: OutputStream -> responseBody.write(payload) }
        }
        server.createContext("/api/job-fit-report") { exchange ->
            val body = exchange.requestBody.bufferedReader().use { it.readText() }
            val root = mapper.readTree(body)
            val audience = root.path("audience").asText("candidate")
            val jobTitle = root.path("jobTitle").asText("Target Role")
            val matchScore = root.path("matchScore").decimalValue().setScale(2)
            val missingSkills = root.path("missingSkills")
                .mapNotNull { node -> node.asText().takeIf { it.isNotBlank() } }
            val narrative = if (audience == "candidate") {
                "岗位适应性报告与技能提升建议：你当前与「$jobTitle」的综合匹配度为 $matchScore%，建议优先补强 ${missingSkills.joinToString(", ").ifBlank { "关键业务成果表达" }}。"
            } else {
                "Candidate-job fit summary for $jobTitle at $matchScore% with gaps in ${missingSkills.joinToString(", ").ifBlank { "none" }}."
            }
            val payload = mapper.writeValueAsBytes(
                mapOf(
                    "headline" to "Stubbed structured fit report",
                    "fitBand" to if (matchScore.toDouble() >= 80.0) "HIGH" else if (matchScore.toDouble() >= 55.0) "MEDIUM" else "LOW",
                    "summary" to "Stub summary for $jobTitle",
                    "strengths" to listOf("Semantic alignment already established"),
                    "risks" to listOf("Missing skills: ${missingSkills.joinToString(", ").ifBlank { "none" }}"),
                    "improvementSuggestions" to listOf("Improve ${missingSkills.firstOrNull() ?: "project impact phrasing"}"),
                    "nextSteps" to listOf("Refresh role-specific project evidence"),
                    "narrative" to narrative,
                ),
            )
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, payload.size.toLong())
            exchange.responseBody.use { responseBody: OutputStream -> responseBody.write(payload) }
        }
        server.start()
        StoppableEmbeddingServer(server)
    }

    private fun buildVector(text: String): List<Double> {
        val dimension = 1536
        val values = DoubleArray(dimension)
        val tokens = text.lowercase().split(Regex("[^a-z0-9+#.]+"))
            .filter { it.isNotBlank() }
            .ifEmpty { listOf("smartats") }

        tokens.forEachIndexed { tokenIndex, token ->
            token.forEachIndexed { charIndex, char ->
                val position = (tokenIndex * 31 + charIndex) % dimension
                values[position] += (char.code % 97) / 97.0
            }
        }

        val norm = sqrt(values.sumOf { it * it }).takeIf { it > 0.0 } ?: 1.0
        return values.map { it / norm }
    }
}