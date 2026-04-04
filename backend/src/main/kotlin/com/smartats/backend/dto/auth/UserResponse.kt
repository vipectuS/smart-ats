package com.smartats.backend.dto.auth

import com.smartats.backend.domain.User
import com.smartats.backend.domain.UserRole
import java.time.Instant
import java.util.UUID

data class UserResponse(
    val id: UUID,
    val username: String,
    val email: String,
    val role: UserRole,
    val createdAt: Instant,
) {
    companion object {
        fun from(user: User): UserResponse {
            return UserResponse(
                id = requireNotNull(user.id),
                username = user.username,
                email = user.email,
                role = user.role,
                createdAt = requireNotNull(user.createdAt),
            )
        }
    }
}