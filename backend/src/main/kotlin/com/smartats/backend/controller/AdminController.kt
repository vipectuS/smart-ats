package com.smartats.backend.controller

import com.smartats.backend.dto.ApiResponse
import com.smartats.backend.dto.admin.AdminOverviewResponse
import com.smartats.backend.dto.admin.AdminParseFailureResponse
import com.smartats.backend.dto.admin.AdminSkillResponse
import com.smartats.backend.dto.admin.AdminSkillUpsertRequest
import com.smartats.backend.service.AdminService
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
class AdminController(
    private val adminService: AdminService,
) {

    @GetMapping("/overview")
    fun getOverview(): ApiResponse<AdminOverviewResponse> {
        return ApiResponse(
            status = HttpStatus.OK.value(),
            data = adminService.getOverview(),
            message = "Success",
        )
    }

    @GetMapping("/skills")
    fun listSkills(): ApiResponse<List<AdminSkillResponse>> {
        return ApiResponse(
            status = HttpStatus.OK.value(),
            data = adminService.listSkills(),
            message = "Success",
        )
    }

    @PostMapping("/skills")
    fun createSkill(@Valid @RequestBody request: AdminSkillUpsertRequest): ResponseEntity<ApiResponse<AdminSkillResponse>> {
        val response = adminService.createSkill(request)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse(status = HttpStatus.CREATED.value(), data = response, message = "Skill created"))
    }

    @PutMapping("/skills/{skillId}")
    fun updateSkill(
        @PathVariable skillId: UUID,
        @Valid @RequestBody request: AdminSkillUpsertRequest,
    ): ApiResponse<AdminSkillResponse> {
        return ApiResponse(
            status = HttpStatus.OK.value(),
            data = adminService.updateSkill(skillId, request),
            message = "Skill updated",
        )
    }

    @GetMapping("/parse-failures")
    fun listParseFailures(
        @RequestParam(defaultValue = "20") limit: Int,
    ): ApiResponse<List<AdminParseFailureResponse>> {
        return ApiResponse(
            status = HttpStatus.OK.value(),
            data = adminService.listParseFailures(limit),
            message = "Success",
        )
    }
}