package com.smartats.backend.config

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.ApplicationContextException
import org.springframework.stereotype.Component

@Component
class GovernanceReadinessRunner(
    private val governanceProperties: GovernanceProperties,
    private val governanceReadinessService: GovernanceReadinessService,
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(GovernanceReadinessRunner::class.java)

    override fun run(args: ApplicationArguments) {
        val findings = governanceReadinessService.evaluate()

        if (findings.isEmpty()) {
            logger.info("Governance readiness check passed with no findings.")
            return
        }

        findings.forEach { finding ->
            when (finding.severity) {
                GovernanceFindingSeverity.WARNING -> logger.warn("[{}] {}", finding.code, finding.message)
                GovernanceFindingSeverity.ERROR -> logger.error("[{}] {}", finding.code, finding.message)
            }
        }

        if (governanceProperties.secretRotation.failStartupOnBlockingFindings &&
            findings.any { it.severity == GovernanceFindingSeverity.ERROR }) {
            throw ApplicationContextException("Governance readiness check found blocking secret configuration errors.")
        }
    }
}