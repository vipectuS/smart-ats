package com.smartats.backend.security.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.smartats.backend.dto.ApiResponse
import com.smartats.backend.exception.ApiErrorCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class RestAccessDeniedHandler(
    private val objectMapper: ObjectMapper,
) : AccessDeniedHandler {

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) {
        response.status = HttpStatus.FORBIDDEN.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        objectMapper.writeValue(
            response.writer,
            ApiResponse(
                status = HttpStatus.FORBIDDEN.value(),
                data = null,
                message = "Access denied",
                code = ApiErrorCode.ACCESS_DENIED.name,
                retryable = false,
                userHint = "请联系管理员确认当前账号权限。",
                traceId = request.requestId.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
            ),
        )
    }
}