package com.smartats.backend.dto.resume

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.NotEmpty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ResumeParsedResultRequest(
    @field:NotEmpty(message = "parsedData must not be empty")
    val parsedData: Map<String, Any>,
)