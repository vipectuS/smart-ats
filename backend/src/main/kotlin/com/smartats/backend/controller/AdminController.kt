package com.smartats.backend.controller

import com.smartats.backend.domain.AccessAuditActionType
import com.smartats.backend.domain.AccessAuditTargetType
import com.smartats.backend.domain.AdminParseFailureReviewStatus
import com.smartats.backend.dto.ApiResponse
import com.smartats.backend.dto.admin.AccessAuditEventResponse
import com.smartats.backend.dto.admin.AccessAuditSummaryResponse
import com.smartats.backend.dto.admin.AdminOrganizationCreateRequest
import com.smartats.backend.dto.admin.AdminOrganizationResponse
import com.smartats.backend.dto.admin.AdminOrganizationTokenResponse
import com.smartats.backend.dto.admin.AdminOrganizationUpdateRequest
import com.smartats.backend.dto.admin.AdminOverviewResponse
import com.smartats.backend.dto.admin.AdminParseFailureBatchActionResponse
import com.smartats.backend.dto.admin.AdminParseFailureBatchReviewRequest
import com.smartats.backend.dto.admin.AdminParseFailureResponse
import com.smartats.backend.dto.admin.AdminParseFailureSummaryResponse
import com.smartats.backend.dto.admin.AdminParseFailureReviewEventResponse
import com.smartats.backend.dto.admin.AdminParseFailureReviewRequest
import com.smartats.backend.dto.admin.AdminSkillResponse
import com.smartats.backend.dto.admin.AdminSkillUpsertRequest
import com.smartats.backend.dto.resume.ResumeParseTriggerResponse
import com.smartats.backend.service.AccessAuditService
import com.smartats.backend.service.AdminService
import com.smartats.backend.service.OrganizationService
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.security.Principal
import java.util.UUID

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
class AdminController(
    private val adminService: AdminService,
    private val organizationService: OrganizationService,
    private val accessAuditService: AccessAuditService,
) {

    @GetMapping("/overview")
    fun getOverview(authentication: Authentication): ApiResponse<AdminOverviewResponse> {
        accessAuditService.recordAdminOverviewAccess(authentication)
        return ApiResponse(
            status = HttpStatus.OK.value(),
            data = adminService.getOverview(),
            message = "Success",
        )
    }

    @GetMapping("/skills")
    fun listSkills(authentication: Authentication): ApiResponse<List<AdminSkillResponse>> {
        accessAuditService.recordSkillDictionaryAccess(authentication)
        return ApiResponse(
            status = HttpStatus.OK.value(),
            data = adminService.listSkills(),
            message = "Success",
        )
    }

    @GetMapping("/organizations")
    fun listOrganizations(authentication: Authentication): ApiResponse<List<AdminOrganizationResponse>> {
        accessAuditService.recordOrganizationDirectoryAccess(authentication)
        return ApiResponse(
            status = HttpStatus.OK.value(),
            data = organizationService.listOrganizations(),
            message = "Success",
        )
    }

    @PostMapping("/organizations")
    fun createOrganization(
        @Valid @RequestBody request: AdminOrganizationCreateRequest,
        authentication: Authentication,
    ): ResponseEntity<ApiResponse<AdminOrganizationTokenResponse>> {
        val response = organizationService.createOrganization(request)
        accessAuditService.recordAccess(
            authentication,
            AccessAuditActionType.ORGANIZATION_CREATED,
            AccessAuditTargetType.ORGANIZATION_DIRECTORY,
            response.organization.id,
        )
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse(status = HttpStatus.CREATED.value(), data = response, message = "Organization created"))
    }

    @PutMapping("/organizations/{organizationId}")
    fun updateOrganization(
        @PathVariable organizationId: UUID,
        @Valid @RequestBody request: AdminOrganizationUpdateRequest,
        authentication: Authentication,
    ): ApiResponse<AdminOrganizationResponse> {
        val response = organizationService.updateOrganization(organizationId, request)
        accessAuditService.recordAccess(
            authentication,
            AccessAuditActionType.ORGANIZATION_UPDATED,
            AccessAuditTargetType.ORGANIZATION_DIRECTORY,
            response.id,
        )
        return ApiResponse(
            status = HttpStatus.OK.value(),
            data = response,
            message = "Organization updated",
        )
    }

    @PostMapping("/organizations/{organizationId}/regenerate-token")
    fun regenerateOrganizationToken(
        @PathVariable organizationId: UUID,
        authentication: Authentication,
    ): ApiResponse<AdminOrganizationTokenResponse> {
        val response = organizationService.regenerateToken(organizationId)
        accessAuditService.recordAccess(
            authentication,
            AccessAuditActionType.ORGANIZATION_TOKEN_REGENERATED,
            AccessAuditTargetType.ORGANIZATION_DIRECTORY,
            response.organization.id,
        )
        return ApiResponse(
            status = HttpStatus.OK.value(),
            data = response,
            message = "Organization token regenerated",
        )
    }

    @PostMapping("/skills")
    fun createSkill(
        @Valid @RequestBody request: AdminSkillUpsertRequest,
        authentication: Authentication,
    ): ResponseEntity<ApiResponse<AdminSkillResponse>> {
        val response = adminService.createSkill(request)
        accessAuditService.recordAccess(
            authentication,
            AccessAuditActionType.SKILL_CREATED,
            AccessAuditTargetType.SKILL_DICTIONARY,
            response.id,
        )
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse(status = HttpStatus.CREATED.value(), data = response, message = "Skill created"))
    }

    @PutMapping("/skills/{skillId}")
    fun updateSkill(
        @PathVariable skillId: UUID,
        @Valid @RequestBody request: AdminSkillUpsertRequest,
        authentication: Authentication,
    ): ApiResponse<AdminSkillResponse> {
        val response = adminService.updateSkill(skillId, request)
        accessAuditService.recordAccess(
            authentication,
            AccessAuditActionType.SKILL_UPDATED,
            AccessAuditTargetType.SKILL_DICTIONARY,
            response.id,
        )
        return ApiResponse(
            status = HttpStatus.OK.value(),
            data = response,
            message = "Skill updated",
        )
    }

    @GetMapping("/parse-failures")
    fun listParseFailures(
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(required = false) reviewStatus: AdminParseFailureReviewStatus?,
        authentication: Authentication,
    ): ApiResponse<List<AdminParseFailureResponse>> {
        val response = adminService.listParseFailures(limit, reviewStatus)
        response.forEach { item ->
            accessAuditService.recordAccess(
                authentication,
                AccessAuditActionType.PARSE_FAILURES_VIEWED,
                AccessAuditTargetType.RESUME,
                item.resumeId,
            )
        }
        return ApiResponse(
            status = HttpStatus.OK.value(),
            data = response,
            message = "Success",
        )
    }

    @GetMapping("/parse-failures/summary")
    fun getParseFailureSummary(authentication: Authentication): ApiResponse<AdminParseFailureSummaryResponse> {
        val response = adminService.getParseFailureSummary()
        accessAuditService.recordAccess(
            authentication,
            AccessAuditActionType.PARSE_FAILURES_VIEWED,
            AccessAuditTargetType.SYSTEM_OVERVIEW,
            AccessAuditService.SYSTEM_OVERVIEW_TARGET_ID,
        )
        return ApiResponse(
            status = HttpStatus.OK.value(),
            data = response,
            message = "Success",
        )
    }

    @GetMapping("/parse-failures/summary/export", produces = ["text/csv"])
    fun exportParseFailureSummary(authentication: Authentication): ResponseEntity<String> {
        val body = adminService.exportParseFailureSummaryCsv()
        accessAuditService.recordAccess(
            authentication,
            AccessAuditActionType.PARSE_FAILURE_REVIEW_EVENTS_EXPORTED,
            AccessAuditTargetType.SYSTEM_OVERVIEW,
            AccessAuditService.SYSTEM_OVERVIEW_TARGET_ID,
        )
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"parse-failure-summary.csv\"")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(body)
    }

    @GetMapping("/parse-failures/{resumeId}/review-events")
    fun listParseFailureReviewEvents(
        @PathVariable resumeId: UUID,
        @RequestParam(defaultValue = "20") limit: Int,
        authentication: Authentication,
    ): ApiResponse<List<AdminParseFailureReviewEventResponse>> {
        val response = adminService.listParseFailureReviewEvents(resumeId, limit)
        accessAuditService.recordAccess(
            authentication,
            AccessAuditActionType.PARSE_FAILURE_REVIEW_EVENTS_VIEWED,
            AccessAuditTargetType.RESUME,
            resumeId,
        )
        return ApiResponse(
            status = HttpStatus.OK.value(),
            data = response,
            message = "Success",
        )
    }

    @GetMapping("/access-audit-events")
    fun listAccessAuditEvents(
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(required = false) actionType: AccessAuditActionType?,
        authentication: Authentication,
    ): ApiResponse<List<AccessAuditEventResponse>> {
        val response = adminService.listAccessAuditEvents(limit, actionType)
        accessAuditService.recordAccess(
            authentication,
            AccessAuditActionType.ACCESS_AUDIT_EVENTS_VIEWED,
            AccessAuditTargetType.SYSTEM_OVERVIEW,
            AccessAuditService.SYSTEM_OVERVIEW_TARGET_ID,
        )
        return ApiResponse(
            status = HttpStatus.OK.value(),
            data = response,
            message = "Success",
        )
    }

    @GetMapping("/access-audit-events/summary")
    fun getAccessAuditSummary(authentication: Authentication): ApiResponse<AccessAuditSummaryResponse> {
        val response = adminService.getAccessAuditSummary()
        accessAuditService.recordAccess(
            authentication,
            AccessAuditActionType.ACCESS_AUDIT_SUMMARY_VIEWED,
            AccessAuditTargetType.SYSTEM_OVERVIEW,
            AccessAuditService.SYSTEM_OVERVIEW_TARGET_ID,
        )
        return ApiResponse(
            status = HttpStatus.OK.value(),
            data = response,
            message = "Success",
        )
    }

    @GetMapping("/access-audit-events/summary/export", produces = ["text/csv"])
    fun exportAccessAuditSummary(authentication: Authentication): ResponseEntity<String> {
        val body = adminService.exportAccessAuditSummaryCsv()
        accessAuditService.recordAccess(
            authentication,
            AccessAuditActionType.ACCESS_AUDIT_SUMMARY_EXPORTED,
            AccessAuditTargetType.SYSTEM_OVERVIEW,
            AccessAuditService.SYSTEM_OVERVIEW_TARGET_ID,
        )
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"access-audit-summary.csv\"")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(body)
    }

    @GetMapping("/parse-failures/{resumeId}/review-events/export", produces = ["text/csv"])
    fun exportParseFailureReviewEvents(
        @PathVariable resumeId: UUID,
        @RequestParam(defaultValue = "100") limit: Int,
        authentication: Authentication,
    ): ResponseEntity<String> {
        val body = adminService.exportParseFailureReviewEventsCsv(resumeId, limit)
        accessAuditService.recordAccess(
            authentication,
            AccessAuditActionType.PARSE_FAILURE_REVIEW_EVENTS_EXPORTED,
            AccessAuditTargetType.RESUME,
            resumeId,
        )
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"parse-failure-review-events-$resumeId.csv\"")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(body)
    }

    @PutMapping("/parse-failures/{resumeId}/review")
    fun updateParseFailureReview(
        @PathVariable resumeId: UUID,
        @Valid @RequestBody request: AdminParseFailureReviewRequest,
        principal: Principal,
        authentication: Authentication,
    ): ApiResponse<AdminParseFailureResponse> {
        val response = adminService.updateParseFailureReview(resumeId, principal.name, request.note, request.reviewStatus)
        accessAuditService.recordAccess(
            authentication,
            AccessAuditActionType.PARSE_FAILURE_REVIEW_UPDATED,
            AccessAuditTargetType.RESUME,
            resumeId,
        )
        return ApiResponse(
            status = HttpStatus.OK.value(),
            data = response,
            message = "Parse failure review updated",
        )
    }

    @PostMapping("/parse-failures/{resumeId}/retry")
    fun retryParseFailure(
        @PathVariable resumeId: UUID,
        principal: Principal,
        authentication: Authentication,
        @RequestBody(required = false) request: AdminParseFailureReviewRequest?,
    ): ApiResponse<ResumeParseTriggerResponse> {
        val response = adminService.retryParseFailure(resumeId, principal.name, request?.note)
        accessAuditService.recordAccess(
            authentication,
            AccessAuditActionType.PARSE_FAILURE_RETRY_QUEUED,
            AccessAuditTargetType.RESUME,
            resumeId,
        )
        return ApiResponse(
            status = HttpStatus.OK.value(),
            data = response,
            message = "Resume parse retry queued",
        )
    }

    @PutMapping("/parse-failures/review")
    fun updateParseFailureReviews(
        @Valid @RequestBody request: AdminParseFailureBatchReviewRequest,
        principal: Principal,
        authentication: Authentication,
    ): ApiResponse<AdminParseFailureBatchActionResponse> {
        val response = adminService.updateParseFailureReviews(
            request.resumeIds,
            principal.name,
            request.note,
            request.reviewStatus,
        )
        request.resumeIds.distinct().forEach { resumeId ->
            accessAuditService.recordAccess(
                authentication,
                AccessAuditActionType.PARSE_FAILURE_REVIEW_UPDATED,
                AccessAuditTargetType.RESUME,
                resumeId,
            )
        }
        return ApiResponse(
            status = HttpStatus.OK.value(),
            data = response,
            message = "Parse failure reviews updated",
        )
    }

    @PostMapping("/parse-failures/retry")
    fun retryParseFailures(
        @Valid @RequestBody request: AdminParseFailureBatchReviewRequest,
        principal: Principal,
        authentication: Authentication,
    ): ApiResponse<AdminParseFailureBatchActionResponse> {
        val response = adminService.retryParseFailures(
            request.resumeIds,
            principal.name,
            request.note,
        )
        request.resumeIds.distinct().forEach { resumeId ->
            accessAuditService.recordAccess(
                authentication,
                AccessAuditActionType.PARSE_FAILURE_RETRY_QUEUED,
                AccessAuditTargetType.RESUME,
                resumeId,
            )
        }
        return ApiResponse(
            status = HttpStatus.OK.value(),
            data = response,
            message = "Parse failures re-queued",
        )
    }
}