package com.smartats.backend.controller

import com.smartats.backend.dto.ApiResponse
import com.smartats.backend.dto.organization.OrganizationRefResponse
import com.smartats.backend.service.AccessAuditService
import com.smartats.backend.service.OrganizationService
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/organizations")
class OrganizationController(
    private val organizationService: OrganizationService,
    private val accessAuditService: AccessAuditService,
) {

    @GetMapping("/public")
    fun listPublicOrganizations(authentication: Authentication?): ApiResponse<List<OrganizationRefResponse>> {
        accessAuditService.recordOrganizationDirectoryAccess(authentication)
        return ApiResponse(
            status = HttpStatus.OK.value(),
            data = organizationService.listPublicOrganizations(),
            message = "Success",
        )
    }
}