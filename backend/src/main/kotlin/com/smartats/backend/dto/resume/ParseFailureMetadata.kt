package com.smartats.backend.dto.resume

data class ParseFailureMetadata(
    val code: String?,
    val reason: String?,
)

private val PARSE_FAILURE_CODE_PATTERN = Regex("^\\[([A-Z0-9_]+)](?:\\s+(.+))?$")

fun parseFailureMetadata(rawValue: String?): ParseFailureMetadata {
    val normalized = rawValue?.trim()?.ifBlank { null }
    if (normalized == null) {
        return ParseFailureMetadata(code = null, reason = null)
    }

    val match = PARSE_FAILURE_CODE_PATTERN.matchEntire(normalized)
    if (match != null) {
        return ParseFailureMetadata(
            code = match.groupValues[1].ifBlank { null },
            reason = match.groupValues.getOrNull(2)?.ifBlank { null },
        )
    }

    return ParseFailureMetadata(code = null, reason = normalized)
}

fun composeParseFailureValue(code: String?, reason: String?): String? {
    val normalizedCode = code?.trim()?.ifBlank { null }
    val normalizedReason = reason?.trim()?.ifBlank { null }
    return when {
        normalizedCode != null && normalizedReason != null -> "[$normalizedCode] $normalizedReason"
        normalizedCode != null -> "[$normalizedCode]"
        else -> normalizedReason
    }
}