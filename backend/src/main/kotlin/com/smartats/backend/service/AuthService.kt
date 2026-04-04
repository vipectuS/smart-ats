package com.smartats.backend.service

import com.smartats.backend.domain.User
import com.smartats.backend.domain.UserRole
import com.smartats.backend.dto.auth.AuthResponse
import com.smartats.backend.dto.auth.LoginRequest
import com.smartats.backend.dto.auth.RegisterRequest
import com.smartats.backend.dto.auth.UserResponse
import com.smartats.backend.exception.BadRequestException
import com.smartats.backend.exception.DuplicateResourceException
import com.smartats.backend.exception.InvalidCredentialsException
import com.smartats.backend.repository.UserRepository
import com.smartats.backend.security.JwtService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
) {

    @Transactional
    fun register(request: RegisterRequest): UserResponse {
        if (request.role == UserRole.ADMIN) {
            throw BadRequestException("Public registration cannot assign ADMIN role")
        }
        if (userRepository.existsByUsername(request.username)) {
            throw DuplicateResourceException("Username already exists")
        }
        if (userRepository.existsByEmail(request.email)) {
            throw DuplicateResourceException("Email already exists")
        }

        val user = User(
            username = request.username,
            passwordHash = passwordEncoder.encode(request.password),
            email = request.email,
            role = request.role,
        )

        return UserResponse.from(userRepository.save(user))
    }

    @Transactional(readOnly = true)
    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByUsername(request.username)
            .orElseThrow { InvalidCredentialsException("Invalid username or password") }

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw InvalidCredentialsException("Invalid username or password")
        }

        return AuthResponse(
            accessToken = jwtService.generateToken(user),
            expiresIn = jwtService.getExpirationMinutes() * 60,
            user = UserResponse.from(user),
        )
    }
}