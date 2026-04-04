package com.smartats.backend.controller

import com.smartats.backend.dto.ApiResponse
import com.smartats.backend.dto.auth.UserResponse
import com.smartats.backend.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService,
) {

    @GetMapping("/me")
    fun me(principal: Principal): ApiResponse<UserResponse> {
        val response = userService.getCurrentUser(principal.name)
        return ApiResponse(status = HttpStatus.OK.value(), data = response, message = "Success")
    }
}