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
            val missingSkillSummary = missingSkills.joinToString("、").ifBlank { "关键业务成果表达" }
            val narrative = when (audience) {
                "candidate" -> "岗位适应性报告与技能提升建议：你当前与「$jobTitle」的综合匹配度为 $matchScore%，建议优先补强 $missingSkillSummary。"
                "hr" -> "岗位候选人适配分析：候选人与「$jobTitle」的综合匹配度为 $matchScore%，当前待补强项为 $missingSkillSummary。"
                else -> "共享岗位匹配分析：当前围绕「$jobTitle」形成的综合匹配度为 $matchScore%，仍需优先补齐 $missingSkillSummary。"
            }
            val payload = mapper.writeValueAsBytes(
                mapOf(
                    "headline" to "测试桩结构化匹配报告",
                    "fitBand" to if (matchScore.toDouble() >= 80.0) "HIGH" else if (matchScore.toDouble() >= 55.0) "MEDIUM" else "LOW",
                    "summary" to "已完成围绕 $jobTitle 的结构化匹配分析。",
                    "strengths" to listOf("当前语义相关性已经建立"),
                    "risks" to listOf("待补强技能：${missingSkills.joinToString("、").ifBlank { "暂无" }}"),
                    "improvementSuggestions" to listOf("优先补强 ${missingSkills.firstOrNull() ?: "项目成果量化表达"}"),
                    "nextSteps" to listOf("补充更贴近岗位的项目证据"),
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