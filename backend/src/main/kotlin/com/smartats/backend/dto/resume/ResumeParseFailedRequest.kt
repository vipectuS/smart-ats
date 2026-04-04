package com.smartats.backend.dto.resume

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.Size

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ResumeParseFailedRequest(
    @field:Size(max = 2000, message = "reason must be at most 2000 characters")
    val reason: String? = null,
)