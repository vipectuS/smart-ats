package com.smartats.backend.security

import com.smartats.backend.config.JwtProperties
import com.smartats.backend.domain.User
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Date

@Service
class JwtService(
    private val jwtProperties: JwtProperties,
) {

    fun generateToken(user: User, rememberMe: Boolean = false): String {
        val now = Instant.now()
        val expirationSeconds = getExpirationSeconds(rememberMe)
        val expiry = now.plusSeconds(expirationSeconds)

        return Jwts.builder()
            .subject(user.username)
            .claim("role", user.role.name)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(signingKey())
            .compact()
    }

    fun extractUsername(token: String): String = extractAllClaims(token).subject

    fun isTokenValid(token: String, userDetails: UserDetails): Boolean {
        val username = extractUsername(token)
        return username == userDetails.username && !isTokenExpired(token)
    }

    fun getExpirationSeconds(rememberMe: Boolean = false): Long {
        return if (rememberMe) {
            7 * 24 * 60 * 60L // 7 days in seconds
        } else {
            jwtProperties.expirationMinutes * 60
        }
    }

    private fun isTokenExpired(token: String): Boolean = extractAllClaims(token).expiration.before(Date())

    private fun extractAllClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(signingKey())
            .build()
            .parseSignedClaims(token)
            .payload
    }

    private fun signingKey() = Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray(StandardCharsets.UTF_8))
}