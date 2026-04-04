package com.smartats.backend.dto.candidate

import jakarta.validation.constraints.Size

data class CandidateProfileUpdateRequest(
    @field:Size(max = 500, message = "GitHub URL must be at most 500 characters")
    val githubUrl: String? = null,

    @field:Size(max = 500, message = "Portfolio URL must be at most 500 characters")
    val portfolioUrl: String? = null,
)