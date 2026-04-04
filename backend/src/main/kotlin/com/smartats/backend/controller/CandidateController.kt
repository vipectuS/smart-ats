package com.smartats.backend.controller

import com.smartats.backend.dto.ApiResponse
import com.smartats.backend.dto.candidate.CandidateJobActionListItemResponse
import com.smartats.backend.dto.candidate.CandidateJobMatchResponse
import com.smartats.backend.dto.candidate.CandidateProfileResponse
import com.smartats.backend.dto.candidate.CandidateProfileUpdateRequest
import com.smartats.backend.service.CandidateJobActionService
import com.smartats.backend.service.CandidateProfileService
import com.smartats.backend.service.RecommendationService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

@RestController
@RequestMapping("/api/candidate")
@PreAuthorize("hasRole('CANDIDATE')")
class CandidateController(
    private val candidateProfileService: CandidateProfileService,
    private val recommendationService: RecommendationService,
    private val candidateJobActionService: CandidateJobActionService,
) {

    @GetMapping("/profile")
    fun getProfile(principal: Principal): ApiResponse<CandidateProfileResponse> {
        val response = candidateProfileService.getCurrentCandidateProfile(principal.name)
        return ApiResponse(status = HttpStatus.OK.value(), data = response, message = "Success")
    }

    @PutMapping("/profile")
    fun updateProfile(
        principal: Principal,
        @Valid @RequestBody request: CandidateProfileUpdateRequest,
    ): ApiResponse<CandidateProfileResponse> {
        val response = candidateProfileService.updateCurrentCandidateProfile(principal.name, request)
        return ApiResponse(status = HttpStatus.OK.value(), data = response, message = "Candidate profile updated")
    }

    @PostMapping("/match-jobs")
    fun matchJobs(principal: Principal): ApiResponse<CandidateJobMatchResponse> {
        val response = recommendationService.generateJobMatchesForCandidate(principal.name)
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