package com.smartats.backend.dto

data class ApiResponse<T>(
    val status: Int,
    val data: T?,
    val message: String,
)