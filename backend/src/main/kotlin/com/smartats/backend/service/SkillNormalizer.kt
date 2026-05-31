package com.smartats.backend.service

import com.smartats.backend.repository.SkillDictionaryRepository
import org.springframework.stereotype.Component

@Component
class SkillNormalizer(
    private val skillDictionaryRepository: SkillDictionaryRepository
) {
    private val normalizationCache = mutableMapOf<String, List<String>>()

    fun normalizeToTokens(rawSkillName: String, tokenizer: (String) -> List<String>): List<String> {
        // Find if this skill exactly or closely matches an alias, if so, map to canonical
        val dict = skillDictionaryRepository.findByEnabledTrueOrderByNameAsc()
        
        val rawLower = rawSkillName.lowercase().trim()
        val rawTokens = tokenizer(rawSkillName)
        val rawTokenString = rawTokens.joinToString(" ")

        for (entry in dict) {
            val canonicalTokens = tokenizer(entry.name)
            if (rawLower == entry.name.lowercase().trim() || rawTokenString == canonicalTokens.joinToString(" ")) {
                return canonicalTokens
            }
            for (alias in entry.aliases) {
                if (rawLower == alias.lowercase().trim() || rawTokenString == tokenizer(alias).joinToString(" ")) {
                    return canonicalTokens
                }
            }
        }
        return rawTokens
    }
}
