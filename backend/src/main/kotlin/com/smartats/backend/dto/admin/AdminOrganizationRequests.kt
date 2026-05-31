package com.smartats.backend.dto.admin

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class AdminOrganizationCreateRequest(
    @field:NotBlank(message = "Organization name is required")
    @field:Size(min = 2, max = 150, message = "Organization name must be between 2 and 150 characters")
    val name: String,
)

data class AdminOrganizationUpdateRequest(
    @field:NotBlank(message = "Organization name is required")
    @field:Size(min = 2, max = 150, message = "Organization name must be between 2 and 150 characters")
    val name: String,
    val enabled: Boolean,
)