package com.smartats.backend.exception

import com.smartats.backend.dto.ApiResponse
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateResourceException::class)
    fun handleDuplicateResource(exception: DuplicateResourceException): ResponseEntity<ApiResponse<Nothing>> {
        return buildResponse(HttpStatus.CONFLICT, exception.message ?: "Resource already exists")
    }

    @ExceptionHandler(InvalidCredentialsException::class)
    fun handleInvalidCredentials(exception: InvalidCredentialsException): ResponseEntity<ApiResponse<Nothing>> {
        return buildResponse(HttpStatus.UNAUTHORIZED, exception.message ?: "Invalid credentials")
    }

    @ExceptionHandler(BadRequestException::class)
    fun handleBadRequest(exception: BadRequestException): ResponseEntity<ApiResponse<Nothing>> {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.message ?: "Bad request")
    }

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleNotFound(exception: ResourceNotFoundException): ResponseEntity<ApiResponse<Nothing>> {
        return buildResponse(HttpStatus.NOT_FOUND, exception.message ?: "Resource not found")
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(exception: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        val message = exception.bindingResult.fieldErrors
            .joinToString(separator = "; ") { "${it.field}: ${it.defaultMessage}" }
            .ifBlank { "Validation failed" }
        return buildResponse(HttpStatus.BAD_REQUEST, message)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(exception: ConstraintViolationException): ResponseEntity<ApiResponse<Nothing>> {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.message ?: "Validation failed")
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableMessage(exception: HttpMessageNotReadableException): ResponseEntity<ApiResponse<Nothing>> {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.message ?: "Malformed JSON request")
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(exception: AccessDeniedException): ResponseEntity<ApiResponse<Nothing>> {
        return buildResponse(HttpStatus.FORBIDDEN, exception.message ?: "Access denied")
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(exception: Exception): ResponseEntity<ApiResponse<Nothing>> {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, exception.message ?: "Internal server error")
    }

    private fun buildResponse(status: HttpStatus, message: String): ResponseEntity<ApiResponse<Nothing>> {
        return ResponseEntity.status(status)
            .body(ApiResponse(status = status.value(), data = null, message = message))
    }
}