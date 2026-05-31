package com.smartats.backend.config

import org.springframework.stereotype.Service
import java.time.LocalDate

enum class GovernanceFindingSeverity {
    WARNING,
    ERROR,
}

data class GovernanceFinding(
    val code: String,
    val severity: GovernanceFindingSeverity,
    val message: String,
)

@Service
class GovernanceReadinessService(
    private val governanceProperties: GovernanceProperties,
    private val jwtProperties: JwtProperties,
    private val internalCallbackProperties: InternalCallbackProperties,
) {
    fun evaluate(now: LocalDate = LocalDate.now()): List<GovernanceFinding> {
        val findings = mutableListOf<GovernanceFinding>()
        val transport = governanceProperties.transportSecurity
        val securityHeaders = governanceProperties.securityHeaders
        val secretRotation = governanceProperties.secretRotation

        if (transport.tlsTerminationExpected && !securityHeaders.enabled) {
            findings += GovernanceFinding(
                code = "SECURITY_HEADERS_DISABLED",
                severity = GovernanceFindingSeverity.WARNING,
                message = "TLS termination is expected, but app.governance.security-headers.enabled=false leaves response hardening disabled.",
            )
        }

        if (transport.tlsTerminationExpected && !securityHeaders.hstsEnabled) {
            findings += GovernanceFinding(
                code = "HSTS_DISABLED_UNDER_TLS",
                severity = GovernanceFindingSeverity.WARNING,
                message = "TLS termination is expected, but HSTS is disabled. Enable HSTS only after HTTPS routing is stable.",
            )
        }

        if (transport.tlsTerminationExpected && transport.forwardedHeadersStrategy.equals("none", ignoreCase = true)) {
            findings += GovernanceFinding(
                code = "FORWARDED_HEADERS_STRATEGY_NONE",
                severity = GovernanceFindingSeverity.WARNING,
                message = "TLS termination is expected, but forwarded headers strategy is none. Reverse-proxy deployments should set SERVER_FORWARD_HEADERS_STRATEGY=framework.",
            )
        }

        if (jwtProperties.secret == DEFAULT_JWT_SECRET) {
            findings += GovernanceFinding(
                code = "DEFAULT_JWT_SECRET",
                severity = GovernanceFindingSeverity.ERROR,
                message = "JWT secret is still using the repository default placeholder. Override JWT_SECRET before shared or production deployment.",
            )
        }

        if (internalCallbackProperties.apiKey == DEFAULT_INTERNAL_CALLBACK_KEY) {
            findings += GovernanceFinding(
                code = "DEFAULT_INTERNAL_CALLBACK_KEY",
                severity = GovernanceFindingSeverity.ERROR,
                message = "Internal callback API key is still using the repository default placeholder. Override INTERNAL_CALLBACK_API_KEY before enabling callbacks beyond local demo mode.",
            )
        }

        if (secretRotation.enabled) {
            findings += evaluateRotationDate(
                codePrefix = "JWT_SECRET",
                label = "JWT secret",
                lastRotatedOn = secretRotation.jwtSecretLastRotatedOn,
                now = now,
                maxSecretAgeDays = secretRotation.maxSecretAgeDays,
            )
            findings += evaluateRotationDate(
                codePrefix = "INTERNAL_CALLBACK_KEY",
                label = "Internal callback key",
                lastRotatedOn = secretRotation.internalCallbackKeyLastRotatedOn,
                now = now,
                maxSecretAgeDays = secretRotation.maxSecretAgeDays,
            )
        }

        return findings
    }

    private fun evaluateRotationDate(
        codePrefix: String,
        label: String,
        lastRotatedOn: LocalDate?,
        now: LocalDate,
        maxSecretAgeDays: Long,
    ): List<GovernanceFinding> {
        if (lastRotatedOn == null) {
            return listOf(
                GovernanceFinding(
                    code = "${codePrefix}_ROTATION_DATE_MISSING",
                    severity = GovernanceFindingSeverity.WARNING,
                    message = "$label last-rotated date is not configured. Record it under app.governance.secret-rotation for auditability.",
                ),
            )
        }

        return if (lastRotatedOn.plusDays(maxSecretAgeDays).isBefore(now)) {
            listOf(
                GovernanceFinding(
                    code = "${codePrefix}_ROTATION_OVERDUE",
                    severity = GovernanceFindingSeverity.WARNING,
                    message = "$label rotation is older than ${maxSecretAgeDays} days and should be rotated.",
                ),
            )
        } else {
            emptyList()
        }
    }

    companion object {
        const val DEFAULT_JWT_SECRET = "change-me-jwt-secret-at-least-32-bytes"
        const val DEFAULT_INTERNAL_CALLBACK_KEY = "change-me-internal-callback-key"
    }
}