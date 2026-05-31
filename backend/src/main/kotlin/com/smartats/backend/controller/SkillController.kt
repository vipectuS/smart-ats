package com.smartats.backend.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.smartats.backend.dto.ApiResponse
import com.smartats.backend.dto.skill.PublicSkillCatalogItemResponse
import com.smartats.backend.repository.SkillDictionaryRepository
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/skills")
class SkillController(
    private val skillDictionaryRepository: SkillDictionaryRepository,
    private val objectMapper: ObjectMapper,
) {
    @GetMapping
    fun listEnabledSkills(): ApiResponse<List<String>> {
        val skills = skillDictionaryRepository.findEnabledNamesOrderByNameAsc()
        return ApiResponse(
            status = HttpStatus.OK.value(),
            data = skills,
            message = "Success"
        )
    }

    @GetMapping("/catalog")
    fun listEnabledSkillCatalog(): ApiResponse<List<PublicSkillCatalogItemResponse>> {
        val rows = skillDictionaryRepository.findEnabledNameAliasRowsOrderByNameAsc()
        val items = rows.map { row ->
            val name = row[0].toString()
            val aliasesJson = row.getOrNull(1)?.toString() ?: "[]"
            PublicSkillCatalogItemResponse(
                name = name,
                aliases = parseAliasesSafely(aliasesJson),
            )
        }

        return ApiResponse(
            status = HttpStatus.OK.value(),
            data = items,
            message = "Success",
        )
    }

    private fun parseAliasesSafely(raw: String): List<String> {
        return try {
            objectMapper.readValue(raw, object : TypeReference<List<String>>() {})
        } catch (_: Exception) {
            emptyList()
        }
    }
}
