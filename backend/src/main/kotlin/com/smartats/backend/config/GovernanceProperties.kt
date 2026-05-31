package com.smartats.backend.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.LocalDate

@ConfigurationProperties(prefix = "app.governance")
data class GovernanceProperties(
    val dataRetention: DataRetentionProperties = DataRetentionProperties(),
    val securityHeaders: SecurityHeadersProperties = SecurityHeadersProperties(),
    val transportSecurity: TransportSecurityProperties = TransportSecurityProperties(),
    val secretRotation: SecretRotationProperties = SecretRotationProperties(),
) {
    data class DataRetentionProperties(
        val enabled: Boolean = false,
        val purgeCron: String = "0 0 3 * * *",
        val accessAuditRetentionDays: Long = 365,
        val parseFailureReviewRetentionDays: Long = 365,
    )

    data class SecurityHeadersProperties(
        val enabled: Boolean = true,
        val hstsEnabled: Boolean = false,
        val hstsMaxAgeSeconds: Long = 31_536_000,
        val includeSubDomains: Boolean = true,
        val contentTypeOptionsEnabled: Boolean = true,
        val frameOptionsMode: String = "DENY",
        val referrerPolicy: String = "strict-origin-when-cross-origin",
    )

    data class TransportSecurityProperties(
        val tlsTerminationExpected: Boolean = false,
        val forwardedHeadersStrategy: String = "none",
        val publicBaseUrl: String? = null,
    )

    data class SecretRotationProperties(
        val enabled: Boolean = true,
        val maxSecretAgeDays: Long = 90,
        val failStartupOnBlockingFindings: Boolean = false,
        val jwtSecretLastRotatedOn: LocalDate? = null,
        val internalCallbackKeyLastRotatedOn: LocalDate? = null,
    )
}