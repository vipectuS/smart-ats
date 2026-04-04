package com.smartats.backend.dto.job

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateJobRequest(
    @field:NotBlank(message = "Title is required")
    @field:Size(max = 255, message = "Title must be at most 255 characters")
    val title: String,

    @field:NotBlank(message = "Description is required")
    val description: String,

    @field:NotEmpty(message = "Requirements must not be empty")
    val requirements: Map<String, Any>,
)