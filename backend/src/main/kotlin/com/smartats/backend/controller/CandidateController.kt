package com.smartats.backend.controller

import com.smartats.backend.domain.AccessAuditActionType
import com.smartats.backend.domain.AccessAuditTargetType
import com.smartats.backend.dto.ApiResponse
import com.smartats.backend.dto.candidate.CandidateJobActionListItemResponse
import com.smartats.backend.dto.candidate.CandidateJobMatchResponse
import com.smartats.backend.dto.candidate.CandidateProfileResponse
import com.smartats.backend.dto.candidate.CandidateResumeDetailResponse
import com.smartats.backend.dto.candidate.CandidateResumeSummaryResponse
import com.smartats.backend.dto.candidate.CandidateProfileUpdateRequest
import com.smartats.backend.dto.resume.ResumeParseTriggerResponse
import com.smartats.backend.service.AccessAuditService
import com.smartats.backend.service.CandidateJobActionService
import com.smartats.backend.service.CandidateProfileService
import com.smartats.backend.service.RecommendationService
import com.smartats.backend.service.ResumeService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.security.core.Authentication
import java.security.Principal
import java.util.UUID

@RestController
@RequestMapping("/api/candidate")
@PreAuthorize("hasRole('CANDIDATE')")
class CandidateController(
    private val candidateProfileService: CandidateProfileService,
    private val recommendationService: RecommendationService,
    private val candidateJobActionService: CandidateJobActionService,
    private val resumeService: ResumeService,
    private val accessAuditService: AccessAuditService,
) {

    @GetMapping("/profile")
    fun getProfile(
        principal: Principal,
        authentication: Authentication,
    ): ApiResponse<CandidateProfileResponse> {
        val response = candidateProfileService.getCurrentCandidateProfile(principal.name)
        accessAuditService.recordSelfAccess(authentication, AccessAuditActionType.CANDIDATE_PROFILE_VIEWED)
        accessAuditService.recordCandidateProfileSensitiveFieldAccess(authentication, response)
        return ApiResponse(status = HttpStatus.OK.value(), data = response, message = "Success")
    }

    @PutMapping("/profile")
    fun updateProfile(
        principal: Principal,
        authentication: Authentication,
        @Valid @RequestBody request: CandidateProfileUpdateRequest,
    ): ApiResponse<CandidateProfileResponse> {
        val response = candidateProfileService.updateCurrentCandidateProfile(principal.name, request)
        accessAuditService.recordCandidateProfileSensitiveFieldAccess(authentication, response)
        return ApiResponse(status = HttpStatus.OK.value(), data = response, message = "Candidate profile updated")
    }

    @GetMapping("/resumes")
    fun listResumes(principal: Principal, authentication: Authentication): ApiResponse<List<CandidateResumeSummaryResponse>> {
        val response = resumeService.listCandidateResumes(principal.name)
        response.forEach { resume ->
            accessAuditService.recordResumeSensitiveFieldAccess(authentication, resume.resumeId, resume.contactInfo, resume.parsedData)
        }
        return ApiResponse(status = HttpStatus.OK.value(), data = response, message = "Success")
    }

    @GetMapping("/resumes/{resumeId}")
    fun getResume(
        principal: Principal,
        @PathVariable resumeId: UUID,
        authentication: Authentication,
    ): ApiResponse<CandidateResumeDetailResponse> {
        val response = resumeService.getCandidateResume(principal.name, resumeId)
        accessAuditService.recordAccess(authentication, AccessAuditActionType.RESUME_VIEWED, AccessAuditTargetType.RESUME, resumeId)
        accessAuditService.recordResumeSensitiveFieldAccess(authentication, resumeId, response.contactInfo, response.parsedData)
        return ApiResponse(status = HttpStatus.OK.value(), data = response, message = "Success")
    }

    @PostMapping("/resumes/{resumeId}/reparse")
    fun reparseResume(
        principal: Principal,
        @PathVariable resumeId: UUID,
    ): ResponseEntity<ApiResponse<ResumeParseTriggerResponse>> {
        val response = resumeService.triggerCandidateResumeParse(principal.name, resumeId)
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(ApiResponse(status = HttpStatus.ACCEPTED.value(), data = response, message = "Candidate resume parse queued"))
    }

    @PostMapping("/match-jobs")
    fun matchJobs(
        principal: Principal,
        authentication: Authentication,
    ): ApiResponse<CandidateJobMatchResponse> {
        val response = recommendationService.generateJobMatchesForCandidate(principal.name)
        accessAuditService.recordSelfAccess(authentication, AccessAuditActionType.CANDIDATE_JOB_MATCHES_VIEWED)
        accessAuditService.recordRecommendationJobDetailsAccess(
            authentication,
            response.recommendations.map { it.jobId },
        )
        return ApiResponse(status = HttpStatus.OK.value(), data = response, message = "Job matches generated")
    }

    @GetMapping("/applications")
    fun listApplications(principal: Principal): ApiResponse<List<CandidateJobActionListItemResponse>> {
        val response = candidateJobActionService.listApplications(principal.name)
        return ApiResponse(status = HttpStatus.OK.value(), data = response, message = "Success")
    }

    @GetMapping("/favorites")
    fun listFavorites(principal: Principal): ApiResponse<List<CandidateJobActionListItemResponse>> {
        val response = candidateJobActionService.listFavorites(principal.name)
        return ApiResponse(status = HttpStatus.OK.value(), data = response, message = "Success")
    }

    @GetMapping("/ignores")
    fun listIgnores(principal: Principal): ApiResponse<List<CandidateJobActionListItemResponse>> {
        val response = candidateJobActionService.listIgnores(principal.name)
        return ApiResponse(status = HttpStatus.OK.value(), data = response, message = "Success")
    }
}