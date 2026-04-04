package com.smartats.backend.queue

import com.smartats.backend.config.ResumeQueueProperties
import com.smartats.backend.service.ResumeService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "app.resume-queue", name = ["listener-enabled"], havingValue = "true")
class ResumeQueueConsumer(
    private val resumeService: ResumeService,
    private val resumeQueueProperties: ResumeQueueProperties,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun handleMessage(message: ResumeParseMessage) {
        logger.info("Received resume parse message: resumeId={}, rawContentReference={}", message.resumeId, message.rawContentReference)

        runCatching {
            resumeService.markParsing(message.resumeId)
            Thread.sleep(resumeQueueProperties.mockProcessingDelayMillis)

            val mockParsedData = mapOf(
                "summary" to "Mock AI parsing completed",
                "source" to message.rawContentReference,
                "skills" to listOf("Communication", "Problem Solving"),
                "score" to 88,
            )

            logger.info("Mock parsing completed for resumeId={}", message.resumeId)
            resumeService.markParsed(message.resumeId, mockParsedData)
        }.onFailure { exception ->
            logger.error("Failed to process resume parse message for resumeId={}", message.resumeId, exception)
            resumeService.markParseFailed(message.resumeId)
        }
    }
}