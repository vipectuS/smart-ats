package com.smartats.backend.dto.resume

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateResumeRequest(
    @field:Size(max = 255, message = "Candidate name must be at most 255 characters")
    val candidateName: String? = null,

    @field:Size(max = 255, message = "Contact info must be at most 255 characters")
    val contactInfo: String? = null,

    @field:NotBlank(message = "Raw content reference is required")
    @field:Size(max = 255, message = "Raw content reference must be at most 255 characters")
    val rawContentReference: String,

    val parsedData: Map<String, Any>? = null,
)