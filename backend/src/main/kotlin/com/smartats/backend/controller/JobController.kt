package com.smartats.backend.controller

import com.smartats.backend.dto.ApiResponse
import com.smartats.backend.dto.PageResponse
import com.smartats.backend.dto.candidate.CandidateJobActionResponse
import com.smartats.backend.dto.job.CreateJobRequest
import com.smartats.backend.dto.job.EvaluationRequestWeightsDTO
import com.smartats.backend.dto.job.JobApplicationReviewItemResponse
import com.smartats.backend.dto.job.JobEvaluationResponse
import com.smartats.backend.dto.job.JobRecommendationResponse
import com.smartats.backend.dto.job.JobResponse
import com.smartats.backend.dto.job.UpdateJobApplicationReviewRequest
import com.smartats.backend.dto.job.UpdateJobRequest
import com.smartats.backend.service.CandidateJobActionService
import com.smartats.backend.service.JobService
import com.smartats.backend.service.RecommendationService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.PutMapping
import java.security.Principal
import java.util.UUID

@RestController
@RequestMapping("/api/jobs")
class JobController(
    private val jobService: JobService,
    private val recommendationService: RecommendationService,
    private val candidateJobActionService: CandidateJobActionService,
) {

    @PostMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    fun createJob(
        principal: Principal,
        @Valid @RequestBody request: CreateJobRequest,
    ): ResponseEntity<ApiResponse<JobResponse>> {
        val response = jobService.createJob(principal.name, request)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse(status = HttpStatus.CREATED.value(), data = response, message = "Job created"))
    }

    @PutMapping("/{jobId}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    fun updateJob(
        @PathVariable jobId: UUID,
        principal: Principal,
        @Valid @RequestBody request: UpdateJobRequest,
    ): ApiResponse<JobResponse> {
        val response = jobService.updateJob(principal.name, jobId, request)
        return ApiResponse(status = HttpStatus.OK.value(), data = response, message = "Job updated")
    }

    @GetMapping
    fun listJobs(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ApiResponse<PageResponse<JobResponse>> {
        val response = jobService.listJobs(page = page, size = size)
        return ApiResponse(status = HttpStatus.OK.value(), data = response, message = "Success")
    }

    @GetMapping("/{jobId}")
    fun getJob(@PathVariable jobId: UUID): ApiResponse<JobResponse> {
        val response = jobService.getJob(jobId)
        return ApiResponse(status = HttpStatus.OK.value(), data = response, message = "Success")
    }

    @GetMapping("/{jobId}/applications")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    fun listJobApplications(
        @PathVariable jobId: UUID,
        principal: Principal,
    ): ApiResponse<List<JobApplicationReviewItemResponse>> {
        val response = jobService.listActiveApplications(principal.name, jobId)
        return ApiResponse(status = HttpStatus.OK.value(), data = response, message = "Success")
    }

    @PutMapping("/{jobId}/applications/{applicationId}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    fun reviewJobApplication(
        @PathVariable jobId: UUID,
        @PathVariable applicationId: UUID,
        principal: Principal,
        @Valid @RequestBody request: UpdateJobApplicationReviewRequest,
    ): ApiResponse<JobApplicationReviewItemResponse> {
        val response = jobService.reviewApplication(principal.name, jobId, applicationId, request)
        return ApiResponse(status = HttpStatus.OK.value(), data = response, message = "Application review updated")
    }

    @GetMapping("/{jobId}/recommendations")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    fun listRecommendations(@PathVariable jobId: UUID): ApiResponse<List<JobRecommendationResponse>> {
        val response = recommendationService.listRecommendationsForJob(jobId)
        return ApiResponse(status = HttpStatus.OK.value(), data = response, message = "Success")
    }

    @PostMapping("/{jobId}/evaluate")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    fun evaluateJob(
        @PathVariable jobId: UUID,
        @Valid @RequestBody(required = false) weights: EvaluationRequestWeightsDTO?,
    ): ApiResponse<JobEvaluationResponse> {
        val response = recommendationService.generateRecommendationsForJob(jobId, weights)
        return ApiResponse(status = HttpStatus.OK.value(), data = response, message = "Recommendations generated")
    }

    @PostMapping("/{jobId}/apply")
    @PreAuthorize("hasRole('CANDIDATE')")
    fun applyToJob(
        @PathVariable jobId: UUID,
        principal: Principal,
    ): ResponseEntity<ApiResponse<CandidateJobActionResponse>> {
        val response = candidateJobActionService.apply(principal.name, jobId)
        val status = if (response.created) HttpStatus.CREATED else HttpStatus.OK
        return ResponseEntity.status(status)
            .body(ApiResponse(status = status.value(), data = response, message = if (response.created) "Application created" else "Application already exists"))
    }

    @PostMapping("/{jobId}/favorite")
    @PreAuthorize("hasRole('CANDIDATE')")
    fun favoriteJob(
        @PathVariable jobId: UUID,
        principal: Principal,
    ): ResponseEntity<ApiResponse<CandidateJobActionResponse>> {
        val response = candidateJobActionService.favorite(principal.name, jobId)
        val status = if (response.created) HttpStatus.CREATED else HttpStatus.OK
        return ResponseEntity.status(status)
            .body(ApiResponse(status = status.value(), data = response, message = if (response.created) "Favorite created" else "Favorite already exists"))
    }

    @PostMapping("/{jobId}/ignore")
    @PreAuthorize("hasRole('CANDIDATE')")
    fun ignoreJob(
        @PathVariable jobId: UUID,
        principal: Principal,
    ): ResponseEntity<ApiResponse<CandidateJobActionResponse>> {
        val response = candidateJobActionService.ignore(principal.name, jobId)
        val status = if (response.created) HttpStatus.CREATED else HttpStatus.OK
        return ResponseEntity.status(status)
            .body(ApiResponse(status = status.value(), data = response, message = if (response.created) "Ignore created" else "Ignore already exists"))
    }

    @DeleteMapping("/{jobId}/favorite")
    @PreAuthorize("hasRole('CANDIDATE')")
    fun unfavoriteJob(
        @PathVariable jobId: UUID,
        principal: Principal,
    ): ApiResponse<CandidateJobActionResponse> {
        val response = candidateJobActionService.unfavorite(principal.name, jobId)
        return ApiResponse(status = HttpStatus.OK.value(), data = response, message = if (response.changed) "Favorite cancelled" else "Favorite already inactive")
    }

    @DeleteMapping("/{jobId}/ignore")
    @PreAuthorize("hasRole('CANDIDATE')")
    fun unignoreJob(
        @PathVariable jobId: UUID,
        principal: Principal,
    ): ApiResponse<CandidateJobActionResponse> {
        val response = candidateJobActionService.unignore(principal.name, jobId)
        return ApiResponse(status = HttpStatus.OK.value(), data = response, message = if (response.changed) "Ignore cancelled" else "Ignore already inactive")
    }

    @DeleteMapping("/{jobId}/apply")
    @PreAuthorize("hasRole('CANDIDATE')")
    fun withdrawApplication(
        @PathVariable jobId: UUID,
        principal: Principal,
    ): ApiResponse<CandidateJobActionResponse> {
        val response = candidateJobActionService.withdraw(principal.name, jobId)
        return ApiResponse(status = HttpStatus.OK.value(), data = response, message = if (response.changed) "Application withdrawn" else "Application already inactive")
    }

    @PostMapping("/{jobId}/withdraw")
    @PreAuthorize("hasRole('CANDIDATE')")
    fun withdrawApplicationAlias(
        @PathVariable jobId: UUID,
        principal: Principal,
    ): ApiResponse<CandidateJobActionResponse> {
        val response = candidateJobActionService.withdraw(principal.name, jobId)
        return ApiResponse(status = HttpStatus.OK.value(), data = response, message = if (response.changed) "Application withdrawn" else "Application already inactive")
    }
}