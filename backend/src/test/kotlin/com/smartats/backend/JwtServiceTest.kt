package com.smartats.backend

import com.smartats.backend.config.JwtProperties
import com.smartats.backend.domain.User
import com.smartats.backend.domain.UserRole
import com.smartats.backend.security.JwtService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User as SecurityUser

class JwtServiceTest {

    private val jwtService = JwtService(
        JwtProperties(
            secret = "test-secret-key-that-is-safely-long-enough-for-hmac-256",
            expirationMinutes = 30,
        ),
    )

    @Test
    fun `generate token exposes expected username and validates principal`() {
        val domainUser = User(
            username = "jwt_user",
            passwordHash = "encoded",
            email = "jwt@example.com",
            role = UserRole.HR,
        )
        val userDetails = SecurityUser(
            "jwt_user",
            "encoded",
            listOf(SimpleGrantedAuthority("ROLE_HR")),
        )

        val token = jwtService.generateToken(domainUser)

        assertEquals("jwt_user", jwtService.extractUsername(token))
        assertTrue(jwtService.isTokenValid(token, userDetails))
        assertFalse(
            jwtService.isTokenValid(
                token,
                SecurityUser("another_user", "encoded", listOf(SimpleGrantedAuthority("ROLE_HR"))),
            ),
        )
    }
}