package com.smartats.backend.dto.admin

import com.smartats.backend.domain.SkillDictionaryEntry
import java.time.LocalDateTime
import java.util.UUID

data class AdminSkillResponse(
    val id: UUID,
    val name: String,
    val category: String?,
    val aliases: List<String>,
    val enabled: Boolean,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(entry: SkillDictionaryEntry): AdminSkillResponse {
            return AdminSkillResponse(
                id = requireNotNull(entry.id),
                name = entry.name,
                category = entry.category,
                aliases = entry.aliases,
                enabled = entry.enabled,
                updatedAt = entry.updatedAt,
            )
        }
    }
}