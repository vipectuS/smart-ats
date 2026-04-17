package com.smartats.backend.dto.resume

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateResumeRequest(
    @field:Size(max = 255, message = "Candidate name must be at most 255 characters")
    val candidateName: String? = null,

    @field:Size(max = 255, message = "Contact info must be at most 255 characters")
    val contactInfo: String? = null,

    @field:NotBlank(message = "Raw content reference is required")
    @field:Size(max = 255, message = "Raw content reference must be at most 255 characters")
    val rawContentReference: String,

    val browserPreprocessedPayload: BrowserResumePreprocessedPayloadRequest? = null,

    val parsedData: Map<String, Any>? = null,
) {
    fun browserPreprocessedPayloadAsMap(): Map<String, Any>? = browserPreprocessedPayload?.toMap()
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class BrowserResumePreprocessedPayloadRequest(
    val engine: String,
    val mode: String,
    val sourceFileName: String,
    val sourceMimeType: String,
    val sourceFileSize: Long,
    val derivedReference: String,
    val pageCount: Int,
    val extractedTextPreview: String? = null,
    val generatedAt: String,
    val warnings: List<String> = emptyList(),
    val pagePreviews: List<BrowserResumePagePreviewRequest> = emptyList(),
) {
    fun toMap(): Map<String, Any> {
        val payload = linkedMapOf<String, Any>(
            "engine" to engine,
            "mode" to mode,
            "sourceFileName" to sourceFileName,
            "sourceMimeType" to sourceMimeType,
            "sourceFileSize" to sourceFileSize,
            "derivedReference" to derivedReference,
            "pageCount" to pageCount,
            "generatedAt" to generatedAt,
            "warnings" to warnings,
            "pagePreviews" to pagePreviews.map { it.toMap() },
        )

        extractedTextPreview?.let { payload["extractedTextPreview"] = it }
        return payload
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class BrowserResumePagePreviewRequest(
    val pageNumber: Int,
    val width: Int,
    val height: Int,
    val imageDataUrl: String,
    val textPreview: String? = null,
) {
    fun toMap(): Map<String, Any> {
        val payload = linkedMapOf<String, Any>(
            "pageNumber" to pageNumber,
            "width" to width,
            "height" to height,
            "imageDataUrl" to imageDataUrl,
        )

        textPreview?.let { payload["textPreview"] = it }
        return payload
    }
}