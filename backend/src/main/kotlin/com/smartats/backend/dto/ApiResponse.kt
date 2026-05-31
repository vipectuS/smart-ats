package com.smartats.backend.dto

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val status: Int,
    val data: T?,
    val message: String,
    val code: String? = null,
    val retryable: Boolean? = null,
    val userHint: String? = null,
    val traceId: String? = null,
)