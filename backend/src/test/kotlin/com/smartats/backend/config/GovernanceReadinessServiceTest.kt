package com.smartats.backend.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class GovernanceReadinessServiceTest {
    @Test
    fun `evaluate returns default secret and transport findings when governance is unsafe`() {
        val service = GovernanceReadinessService(
            governanceProperties = GovernanceProperties(
                securityHeaders = GovernanceProperties.SecurityHeadersProperties(
                    enabled = false,
                    hstsEnabled = false,
                ),
                transportSecurity = GovernanceProperties.TransportSecurityProperties(
                    tlsTerminationExpected = true,
                    forwardedHeadersStrategy = "none",
                ),
                secretRotation = GovernanceProperties.SecretRotationProperties(
                    enabled = true,
                    maxSecretAgeDays = 90,
                    jwtSecretLastRotatedOn = null,
                    internalCallbackKeyLastRotatedOn = LocalDate.of(2025, 1, 1),
                ),
            ),
            jwtProperties = JwtProperties(
                secret = GovernanceReadinessService.DEFAULT_JWT_SECRET,
                expirationMinutes = 120,
            ),
            internalCallbackProperties = InternalCallbackProperties(
                headerName = "X-Internal-Api-Key",
                apiKey = GovernanceReadinessService.DEFAULT_INTERNAL_CALLBACK_KEY,
            ),
        )

        val findings = service.evaluate(LocalDate.of(2026, 5, 15))
        val codes = findings.map { it.code }.toSet()

        assertTrue(codes.contains("SECURITY_HEADERS_DISABLED"))
        assertTrue(codes.contains("HSTS_DISABLED_UNDER_TLS"))
        assertTrue(codes.contains("FORWARDED_HEADERS_STRATEGY_NONE"))
        assertTrue(codes.contains("DEFAULT_JWT_SECRET"))
        assertTrue(codes.contains("DEFAULT_INTERNAL_CALLBACK_KEY"))
        assertTrue(codes.contains("JWT_SECRET_ROTATION_DATE_MISSING"))
        assertTrue(codes.contains("INTERNAL_CALLBACK_KEY_ROTATION_OVERDUE"))
        assertEquals(7, findings.size)
    }

    @Test
    fun `evaluate returns no findings when transport and secret governance are configured`() {
        val service = GovernanceReadinessService(
            governanceProperties = GovernanceProperties(
                securityHeaders = GovernanceProperties.SecurityHeadersProperties(
                    enabled = true,
                    hstsEnabled = true,
                ),
                transportSecurity = GovernanceProperties.TransportSecurityProperties(
                    tlsTerminationExpected = true,
                    forwardedHeadersStrategy = "framework",
                    publicBaseUrl = "https://smart-ats.example.com",
                ),
                secretRotation = GovernanceProperties.SecretRotationProperties(
                    enabled = true,
                    maxSecretAgeDays = 90,
                    jwtSecretLastRotatedOn = LocalDate.of(2026, 5, 1),
                    internalCallbackKeyLastRotatedOn = LocalDate.of(2026, 5, 1),
                ),
            ),
            jwtProperties = JwtProperties(
                secret = "local-demo-jwt-secret-that-is-not-the-default-placeholder",
                expirationMinutes = 120,
            ),
            internalCallbackProperties = InternalCallbackProperties(
                headerName = "X-Internal-Api-Key",
                apiKey = "local-demo-internal-callback-key-that-is-not-the-default",
            ),
        )

        val findings = service.evaluate(LocalDate.of(2026, 5, 15))

        assertTrue(findings.isEmpty())
    }
}