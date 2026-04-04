package com.smartats.backend.controller

import com.smartats.backend.dto.ApiResponse
import com.smartats.backend.dto.resume.ResumeStatusResponse
import com.smartats.backend.dto.resume.CreateResumeRequest
import com.smartats.backend.dto.resume.ResumeParseTriggerResponse
import com.smartats.backend.dto.resume.ResumeResponse
import com.smartats.backend.service.ResumeService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestParam
import com.smartats.backend.dto.PageResponse
import org.springframework.security.access.prepost.PreAuthorize
import java.util.UUID
import java.security.Principal

@RestController
@RequestMapping("/api/resumes")
class ResumeController(
    private val resumeService: ResumeService,
) {

    @PostMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    fun createResume(@Valid @RequestBody request: CreateResumeRequest): ResponseEntity<ApiResponse<ResumeResponse>> {
        val response = resumeService.createResume(request)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse(status = HttpStatus.CREATED.value(), data = response, message = "Resume created"))
    }

    @PostMapping("/upload")
    @PreAuthorize("hasRole('CANDIDATE')")
    fun uploadResume(
        principal: Principal,
        @Valid @RequestBody request: CreateResumeRequest,
    ): ResponseEntity<ApiResponse<ResumeResponse>> {
        val response = resumeService.createResumeForCandidate(principal.name, request)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse(status = HttpStatus.CREATED.value(), data = response, message = "Resume uploaded"))
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    fun listResumes(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ApiResponse<PageResponse<ResumeResponse>> {
        val response = resumeService.listResumes(page, size)
        return ApiResponse(status = HttpStatus.OK.value(), data = response, message = "Success")
    }

    @GetMapping("/{resumeId}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    fun getResume(@PathVariable resumeId: UUID): ApiResponse<ResumeResponse> {
        val response = resumeService.getResume(resumeId)
        return ApiResponse(status = HttpStatus.OK.value(), data = response, message = "Success")
    }

    @GetMapping("/{resumeId}/status")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    fun getResumeStatus(@PathVariable resumeId: UUID): ApiResponse<ResumeStatusResponse> {
        val response = resumeService.getResumeStatus(resumeId)
        return ApiResponse(status = HttpStatus.OK.value(), data = response, message = "Success")
    }

    @PostMapping("/{resumeId}/parse")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    fun triggerParse(@PathVariable resumeId: UUID): ResponseEntity<ApiResponse<ResumeParseTriggerResponse>> {
        val response = resumeService.triggerParse(resumeId)
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(ApiResponse(status = HttpStatus.ACCEPTED.value(), data = response, message = "Resume parse queued"))
    }
}