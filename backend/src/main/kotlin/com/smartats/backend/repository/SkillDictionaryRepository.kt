package com.smartats.backend.repository

import com.smartats.backend.domain.SkillDictionaryEntry
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SkillDictionaryRepository : JpaRepository<SkillDictionaryEntry, UUID> {
    fun existsByNameIgnoreCase(name: String): Boolean
    fun existsByIdNotAndNameIgnoreCase(id: UUID, name: String): Boolean
    fun findAllByOrderByEnabledDescNameAsc(): List<SkillDictionaryEntry>
}