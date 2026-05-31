package com.smartats.backend.exception

import com.smartats.backend.dto.ApiResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.util.UUID

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateResourceException::class)
    fun handleDuplicateResource(
        exception: DuplicateResourceException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Nothing>> {
        return buildResponse(HttpStatus.CONFLICT, exception.message ?: "Resource already exists", request, exception)
    }

    @ExceptionHandler(InvalidCredentialsException::class)
    fun handleInvalidCredentials(
        exception: InvalidCredentialsException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Nothing>> {
        return buildResponse(HttpStatus.UNAUTHORIZED, exception.message ?: "Invalid credentials", request, exception)
    }

    @ExceptionHandler(BadRequestException::class)
    fun handleBadRequest(
        exception: BadRequestException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Nothing>> {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.message ?: "Bad request", request, exception)
    }

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleNotFound(
        exception: ResourceNotFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Nothing>> {
        return buildResponse(HttpStatus.NOT_FOUND, exception.message ?: "Resource not found", request, exception)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(
        exception: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Nothing>> {
        val message = exception.bindingResult.fieldErrors
            .joinToString(separator = "; ") { "${it.field}: ${it.defaultMessage}" }
            .ifBlank { "Validation failed" }
        return buildResponse(
            status = HttpStatus.BAD_REQUEST,
            message = message,
            request = request,
            errorCode = ApiErrorCode.VALIDATION_FAILED,
            retryable = true,
            userHint = "请按接口约束修正字段后重试。",
        )
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(
        exception: ConstraintViolationException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Nothing>> {
        return buildResponse(
            status = HttpStatus.BAD_REQUEST,
            message = exception.message ?: "Validation failed",
            request = request,
            errorCode = ApiErrorCode.VALIDATION_FAILED,
            retryable = true,
            userHint = "请按接口约束修正字段后重试。",
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableMessage(
        exception: HttpMessageNotReadableException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Nothing>> {
        return buildResponse(
            status = HttpStatus.BAD_REQUEST,
            message = exception.message ?: "Malformed JSON request",
            request = request,
            errorCode = ApiErrorCode.MALFORMED_JSON_REQUEST,
            retryable = true,
            userHint = "请检查请求 JSON 格式后重试。",
        )
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(
        exception: AccessDeniedException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Nothing>> {
        return buildResponse(
            status = HttpStatus.FORBIDDEN,
            message = exception.message ?: "Access denied",
            request = request,
            errorCode = ApiErrorCode.ACCESS_DENIED,
            retryable = false,
            userHint = "请确认当前账号是否具备访问权限。",
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        exception: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Nothing>> {
        return buildResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            message = exception.message ?: "Internal server error",
            request = request,
            errorCode = ApiErrorCode.INTERNAL_SERVER_ERROR,
            retryable = false,
            userHint = "系统发生异常，请稍后重试或联系管理员。",
        )
    }

    private fun buildResponse(
        status: HttpStatus,
        message: String,
        request: HttpServletRequest,
        exception: ApiException,
    ): ResponseEntity<ApiResponse<Nothing>> {
        return buildResponse(status, message, request, exception.errorCode, exception.retryable, exception.userHint)
    }

    private fun buildResponse(
        status: HttpStatus,
        message: String,
        request: HttpServletRequest,
        errorCode: ApiErrorCode,
        retryable: Boolean,
        userHint: String?,
    ): ResponseEntity<ApiResponse<Nothing>> {
        return ResponseEntity.status(status)
            .body(
                ApiResponse(
                    status = status.value(),
                    data = null,
                    message = message,
                    code = errorCode.name,
                    retryable = retryable,
                    userHint = userHint,
                    traceId = resolveTraceId(request),
                ),
            )
    }

    private fun resolveTraceId(request: HttpServletRequest): String {
        return request.requestId.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
    }
}