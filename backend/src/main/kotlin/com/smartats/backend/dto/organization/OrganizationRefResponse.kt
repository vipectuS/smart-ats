package com.smartats.backend.dto.organization

import com.smartats.backend.domain.Organization
import java.util.UUID

data class OrganizationRefResponse(
    val id: UUID,
    val name: String,
) {
    companion object {
        fun from(organization: Organization): OrganizationRefResponse {
            return OrganizationRefResponse(
                id = requireNotNull(organization.id),
                name = organization.name,
            )
        }
    }
}