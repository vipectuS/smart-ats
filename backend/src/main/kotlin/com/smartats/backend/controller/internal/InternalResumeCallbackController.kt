package com.smartats.backend.controller.internal

import com.smartats.backend.config.InternalCallbackProperties
import com.smartats.backend.dto.ApiResponse
import com.smartats.backend.dto.resume.ResumeParseFailedRequest
import com.smartats.backend.dto.resume.ResumeParsedResultRequest
import com.smartats.backend.dto.resume.ResumeStatusResponse
import com.smartats.backend.service.ResumeService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/internal/api/resumes")
class InternalResumeCallbackController(
    private val resumeService: ResumeService,
    private val internalCallbackProperties: InternalCallbackProperties,
) {

    @PostMapping("/{resumeId}/parsed-result")
    fun submitParsedResult(
        @PathVariable resumeId: UUID,
        @RequestHeader(name = "X-Internal-Api-Key", required = false) apiKey: String?,
        @Valid @RequestBody request: ResumeParsedResultRequest,
    ): ResponseEntity<ApiResponse<ResumeStatusResponse>> {
        val response = resumeService.applyParsedResult(resumeId, apiKey, request)
        return ResponseEntity.status(HttpStatus.OK)
            .body(
                ApiResponse(
                    status = HttpStatus.OK.value(),
                    data = response,
                    message = "Parsed result accepted via ${internalCallbackProperties.headerName}",
                ),
            )
    }

    @PostMapping("/{resumeId}/parse-failed")
    fun submitParseFailed(
        @PathVariable resumeId: UUID,
        @RequestHeader(name = "X-Internal-Api-Key", required = false) apiKey: String?,
        @Valid @RequestBody request: ResumeParseFailedRequest,
    ): ResponseEntity<ApiResponse<ResumeStatusResponse>> {
        val response = resumeService.applyParseFailedResult(resumeId, apiKey, request)
        return ResponseEntity.status(HttpStatus.OK)
            .body(
                ApiResponse(
                    status = HttpStatus.OK.value(),
                    data = response,
                    message = "Parse failure accepted via ${internalCallbackProperties.headerName}",
                ),
            )
    }
}