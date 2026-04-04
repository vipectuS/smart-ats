package com.smartats.backend.controller

import com.smartats.backend.dto.ApiResponse
import com.smartats.backend.dto.hr.HrDashboardStatsResponse
import com.smartats.backend.service.HrDashboardService
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/hr/dashboard")
@PreAuthorize("hasAnyRole('HR', 'ADMIN')")
class HrDashboardController(
    private val hrDashboardService: HrDashboardService,
) {

    @GetMapping("/stats")
    fun getStats(
        @RequestParam(defaultValue = "7") days: Int,
    ): ApiResponse<HrDashboardStatsResponse> {
        val response = hrDashboardService.getStats(days)
        return ApiResponse(status = HttpStatus.OK.value(), data = response, message = "Success")
    }
}