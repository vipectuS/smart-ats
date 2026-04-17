package com.smartats.backend.dto.job

import com.smartats.backend.domain.JobApplicationStatus
import jakarta.validation.constraints.Size

data class UpdateJobApplicationReviewRequest(
    val status: JobApplicationStatus,
    @field:Size(max = 2000)
    val reviewNote: String? = null,
)