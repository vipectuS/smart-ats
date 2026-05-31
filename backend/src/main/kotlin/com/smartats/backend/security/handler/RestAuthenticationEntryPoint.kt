package com.smartats.backend.security.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.smartats.backend.dto.ApiResponse
import com.smartats.backend.exception.ApiErrorCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class RestAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper,
) : AuthenticationEntryPoint {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        response.status = HttpStatus.UNAUTHORIZED.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        objectMapper.writeValue(
            response.writer,
            ApiResponse(
                status = HttpStatus.UNAUTHORIZED.value(),
                data = null,
                message = "Authentication required",
                code = ApiErrorCode.AUTHENTICATION_REQUIRED.name,
                retryable = true,
                userHint = "请先登录后再访问该资源。",
                traceId = request.requestId.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
            ),
        )
    }
}