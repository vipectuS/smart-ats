package com.smartats.backend.dto.auth

import com.smartats.backend.domain.UserRole
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:NotBlank(message = "Username is required")
    @field:Size(min = 4, max = 100, message = "Username must be between 4 and 100 characters")
    val username: String,

    @field:Email(message = "Email must be valid")
    @field:NotBlank(message = "Email is required")
    val email: String,

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
    @field:Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
        message = "Password must contain letters and digits",
    )
    val password: String,

    val role: UserRole = UserRole.CANDIDATE,
)