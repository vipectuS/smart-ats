package com.smartats.backend.controller

import com.smartats.backend.dto.ApiResponse
import com.smartats.backend.dto.auth.AuthResponse
import com.smartats.backend.dto.auth.LoginRequest
import com.smartats.backend.dto.auth.RegisterRequest
import com.smartats.backend.dto.auth.UserResponse
import com.smartats.backend.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(value = ["/api/v1/auth", "/api/auth"])
class AuthController(
    private val authService: AuthService,
) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<ApiResponse<UserResponse>> {
        val response = authService.register(request)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse(status = HttpStatus.CREATED.value(), data = response, message = "User registered"))
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<ApiResponse<AuthResponse>> {
        val response = authService.login(request)
        return ResponseEntity.ok(ApiResponse(status = HttpStatus.OK.value(), data = response, message = "Login successful"))
    }
}