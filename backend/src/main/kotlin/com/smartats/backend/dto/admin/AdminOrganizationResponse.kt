package com.smartats.backend.dto.admin

import com.smartats.backend.domain.Organization
import java.time.Instant
import java.util.UUID

data class AdminOrganizationResponse(
    val id: UUID,
    val name: String,
    val enabled: Boolean,
    val systemDefault: Boolean,
    val tokenPreview: String,
    val hrCount: Long,
    val jobCount: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(organization: Organization, hrCount: Long, jobCount: Long): AdminOrganizationResponse {
            return AdminOrganizationResponse(
                id = requireNotNull(organization.id),
                name = organization.name,
                enabled = organization.enabled,
                systemDefault = organization.isSystemDefault,
                tokenPreview = organization.tokenPreview,
                hrCount = hrCount,
                jobCount = jobCount,
                createdAt = requireNotNull(organization.createdAt),
                updatedAt = requireNotNull(organization.updatedAt),
            )
        }
    }
}

data class AdminOrganizationTokenResponse(
    val organization: AdminOrganizationResponse,
    val generatedToken: String,
)