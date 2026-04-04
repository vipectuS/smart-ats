package com.smartats.backend.dto.auth

data class AuthResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val user: UserResponse,
)