package com.smartats.backend.dto.admin

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class AdminSkillUpsertRequest(
    @field:NotBlank(message = "Skill name is required")
    @field:Size(max = 120, message = "Skill name must be at most 120 characters")
    val name: String,

    @field:Size(max = 80, message = "Category must be at most 80 characters")
    val category: String? = null,

    val aliases: List<String> = emptyList(),

    val enabled: Boolean = true,
)