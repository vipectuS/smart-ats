package com.smartats.backend.repository

import com.smartats.backend.domain.SkillDictionaryEntry
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SkillDictionaryRepository : JpaRepository<SkillDictionaryEntry, UUID> {
    fun existsByNameIgnoreCase(name: String): Boolean
    fun existsByIdNotAndNameIgnoreCase(id: UUID, name: String): Boolean
    fun findAllByOrderByEnabledDescNameAsc(): List<SkillDictionaryEntry>
    fun findByEnabledTrueOrderByNameAsc(): List<SkillDictionaryEntry>

    @Query("select s.name from SkillDictionaryEntry s where s.enabled = true order by s.name asc")
    fun findEnabledNamesOrderByNameAsc(): List<String>

    @Query(value = "select name, cast(aliases as varchar) from skill_dictionary where enabled = true order by name asc", nativeQuery = true)
    fun findEnabledNameAliasRowsOrderByNameAsc(): List<Array<Any>>
}