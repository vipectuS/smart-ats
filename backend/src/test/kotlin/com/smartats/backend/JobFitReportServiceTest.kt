package com.smartats.backend

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.smartats.backend.config.EmbeddingProperties
import com.smartats.backend.dto.xai.JobFitReportRequest
import com.smartats.backend.service.JobFitReportService
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.OutputStream
import java.math.BigDecimal
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicReference

class JobFitReportServiceTest {

    @Test
    fun `generate posts non-empty json over http11 and parses structured response`() {
        val service = JobFitReportService(
            embeddingProperties = EmbeddingProperties(
                aiServiceBaseUrl = "http://127.0.0.1:${server.address.port}",
                requestTimeoutMillis = 3000,
            ),
            objectMapper = mapper,
        )

        val response = service.generate(
            JobFitReportRequest(
                audience = "candidate",
                candidateName = "Alice",
                jobTitle = "Java Platform Engineer",
                jobDescription = "Build Java services with Docker",
                jobRequirements = mapOf("skills" to listOf("Java", "Docker")),
                matchScore = BigDecimal("82"),
                semanticScore = BigDecimal("78"),
                skillScore = BigDecimal("85"),
                experienceScore = BigDecimal("80"),
                educationScore = BigDecimal("72"),
                matchedSkills = listOf("Java"),
                missingSkills = listOf("Docker"),
            ),
        )

        assertEquals("HTTP/1.1", capturedProtocol.get())
        assertEquals("POST", capturedMethod.get())
        assertEquals("application/json", capturedContentType.get())
        assertTrue((capturedBody.get() ?: "").contains("\"candidateName\":\"Alice\""))
        assertTrue((capturedBody.get() ?: "").contains("\"jobTitle\":\"Java Platform Engineer\""))
        assertEquals("MEDIUM", response.fitBand)
        assertTrue(response.narrative.contains("Docker"))
    }

    companion object {
        private val mapper = jacksonObjectMapper()
        private val capturedBody = AtomicReference<String>()
        private val capturedMethod = AtomicReference<String>()
        private val capturedProtocol = AtomicReference<String>()
        private val capturedContentType = AtomicReference<String>()

        @JvmStatic
        private val server: HttpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/api/job-fit-report") { exchange ->
                capturedMethod.set(exchange.requestMethod)
                capturedProtocol.set(exchange.protocol)
                capturedContentType.set(exchange.requestHeaders.getFirst("Content-Type"))
                capturedBody.set(exchange.requestBody.bufferedReader().use { it.readText() })

                val payload = mapper.writeValueAsBytes(
                    mapOf(
                        "headline" to "Structured fit report",
                        "fitBand" to "MEDIUM",
                        "summary" to "You currently match 82% of this role.",
                        "strengths" to listOf("Java already aligns well"),
                        "risks" to listOf("Docker is still missing"),
                        "improvementSuggestions" to listOf("Add one Dockerized backend project"),
                        "nextSteps" to listOf("Refresh your resume with deployment evidence"),
                        "narrative" to "岗位适应性报告与技能提升建议：你已经较强匹配该岗位，但补强 Docker 会进一步提升胜率。",
                    ),
                )
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(200, payload.size.toLong())
                exchange.responseBody.use { output: OutputStream -> output.write(payload) }
            }
            start()
        }

        @JvmStatic
        @AfterAll
        fun shutdownServer() {
            server.stop(0)
        }
    }
}