package com.smartats.backend.controller

import com.smartats.backend.domain.AccessAuditActionType
import com.smartats.backend.dto.ApiResponse
import com.smartats.backend.dto.auth.UserResponse
import com.smartats.backend.service.AccessAuditService
import com.smartats.backend.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService,
    private val accessAuditService: AccessAuditService,
) {

    @GetMapping("/me")
    fun me(authentication: Authentication): ApiResponse<UserResponse> {
        val response = userService.getCurrentUser(authentication.name)
        accessAuditService.recordSelfAccess(authentication, AccessAuditActionType.USER_ACCOUNT_VIEWED)
        accessAuditService.recordCurrentUserAccountSensitiveFieldAccess(authentication)
        return ApiResponse(status = HttpStatus.OK.value(), data = response, message = "Success")
    }
}