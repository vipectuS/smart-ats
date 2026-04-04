package com.smartats.backend.controller

import com.smartats.backend.dto.ApiResponse
import com.smartats.backend.dto.candidate.TimelineEventDTO
import com.smartats.backend.service.CandidateJobActionService
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

@RestController
@RequestMapping("/api/candidates/me")
@PreAuthorize("hasRole('CANDIDATE')")
class CandidateMeController(
    private val candidateJobActionService: CandidateJobActionService,
) {

    @GetMapping("/timeline")
    fun getTimeline(principal: Principal): ApiResponse<List<TimelineEventDTO>> {
        val response = candidateJobActionService.listTimeline(principal.name)
        return ApiResponse(status = HttpStatus.OK.value(), data = response, message = "Success")
    }
}