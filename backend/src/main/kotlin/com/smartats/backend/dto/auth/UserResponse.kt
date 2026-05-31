package com.smartats.backend.dto.auth

import com.smartats.backend.domain.User
import com.smartats.backend.domain.UserRole
import com.smartats.backend.dto.organization.OrganizationRefResponse
import java.time.Instant
import java.util.UUID

data class UserResponse(
    val id: UUID,
    val username: String,
    val email: String,
    val role: UserRole,
    val organization: OrganizationRefResponse?,
    val createdAt: Instant,
) {
    companion object {
        fun from(user: User): UserResponse {
            return UserResponse(
                id = requireNotNull(user.id),
                username = user.username,
                email = user.email,
                role = user.role,
                organization = user.organization?.let(OrganizationRefResponse::from),
                createdAt = requireNotNull(user.createdAt),
            )
        }
    }
}