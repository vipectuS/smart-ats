package com.smartats.backend.service

import com.smartats.backend.domain.Organization
import com.smartats.backend.domain.User
import com.smartats.backend.domain.UserRole
import com.smartats.backend.dto.admin.AdminOrganizationCreateRequest
import com.smartats.backend.dto.admin.AdminOrganizationResponse
import com.smartats.backend.dto.admin.AdminOrganizationTokenResponse
import com.smartats.backend.dto.admin.AdminOrganizationUpdateRequest
import com.smartats.backend.dto.organization.OrganizationRefResponse
import com.smartats.backend.exception.BadRequestException
import com.smartats.backend.exception.DuplicateResourceException
import com.smartats.backend.exception.ResourceNotFoundException
import com.smartats.backend.repository.JobRepository
import com.smartats.backend.repository.OrganizationRepository
import com.smartats.backend.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.util.UUID

@Service
class OrganizationService(
    private val organizationRepository: OrganizationRepository,
    private val userRepository: UserRepository,
    private val jobRepository: JobRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    companion object {
        const val DEFAULT_ORGANIZATION_NAME: String = "Whoseyards Foundation"
        private const val TOKEN_LENGTH = 24
        private const val TOKEN_PREFIX = "org_"
        private const val TOKEN_PREVIEW_MASK = "****"
        private val TOKEN_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789abcdefghijkmnopqrstuvwxyz".toCharArray()
        val LEGACY_ORGANIZATION_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val SYSTEM_DEFAULT_ORGANIZATION_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000002")
    }

    private val secureRandom = SecureRandom()

    @Transactional(readOnly = true)
    fun listPublicOrganizations(): List<OrganizationRefResponse> {
        return organizationRepository.findAllByEnabledTrueOrderByNameAsc()
            .map(OrganizationRefResponse::from)
    }

    @Transactional(readOnly = true)
    fun listOrganizations(): List<AdminOrganizationResponse> {
        return organizationRepository.findAllByOrderByNameAsc().map(::toAdminResponse)
    }

    @Transactional
    fun createOrganization(request: AdminOrganizationCreateRequest): AdminOrganizationTokenResponse {
        val normalizedName = normalizeName(request.name)
        if (organizationRepository.existsByNameIgnoreCase(normalizedName)) {
            throw DuplicateResourceException("Organization already exists")
        }

        val rawToken = generateToken()
        val organization = organizationRepository.save(
            Organization(
                name = normalizedName,
                tokenHash = passwordEncoder.encode(rawToken),
                tokenPreview = buildTokenPreview(rawToken),
                enabled = true,
            ),
        )

        return AdminOrganizationTokenResponse(
            organization = toAdminResponse(organization),
            generatedToken = rawToken,
        )
    }

    @Transactional
    fun updateOrganization(organizationId: UUID, request: AdminOrganizationUpdateRequest): AdminOrganizationResponse {
        val organization = organizationRepository.findById(organizationId)
            .orElseThrow { ResourceNotFoundException("Organization not found") }
        val normalizedName = normalizeName(request.name)
        if (organizationRepository.existsByIdNotAndNameIgnoreCase(organizationId, normalizedName)) {
            throw DuplicateResourceException("Organization already exists")
        }
        if (organization.isSystemDefault && organization.enabled != request.enabled) {
            throw BadRequestException("System default organization cannot be disabled")
        }

        organization.name = normalizedName
        organization.enabled = request.enabled
        return toAdminResponse(organizationRepository.save(organization))
    }

    @Transactional
    fun regenerateToken(organizationId: UUID): AdminOrganizationTokenResponse {
        val organization = organizationRepository.findById(organizationId)
            .orElseThrow { ResourceNotFoundException("Organization not found") }
        val rawToken = generateToken()
        organization.tokenHash = passwordEncoder.encode(rawToken)
        organization.tokenPreview = buildTokenPreview(rawToken)
        val savedOrganization = organizationRepository.save(organization)
        return AdminOrganizationTokenResponse(
            organization = toAdminResponse(savedOrganization),
            generatedToken = rawToken,
        )
    }

    @Transactional(readOnly = true)
    fun resolveOrganizationForHrRegistration(organizationId: UUID?, organizationToken: String?): Organization {
        if (organizationId == null) {
            throw BadRequestException("HR registration requires selecting an organization")
        }
        if (organizationToken.isNullOrBlank()) {
            throw BadRequestException("HR registration requires a valid organization token")
        }

        val organization = organizationRepository.findById(organizationId)
            .orElseThrow { ResourceNotFoundException("Organization not found") }
        if (!organization.enabled) {
            throw BadRequestException("Selected organization is not accepting HR registrations")
        }
        if (!passwordEncoder.matches(organizationToken.trim(), organization.tokenHash)) {
            throw BadRequestException("Organization token is invalid")
        }

        return organization
    }

    @Transactional(readOnly = true)
    fun resolveHrOrganization(user: User): Organization {
        if (user.role != UserRole.HR && user.role != UserRole.ADMIN) {
            throw BadRequestException("Only HR/ADMIN users can be resolved to an organization")
        }
        return user.organization ?: getLegacyOrganization()
    }

    @Transactional(readOnly = true)
    fun getLegacyOrganization(): Organization {
        return organizationRepository.findById(LEGACY_ORGANIZATION_ID)
            .orElseThrow { ResourceNotFoundException("Legacy organization is missing") }
    }

    private fun toAdminResponse(organization: Organization): AdminOrganizationResponse {
        val organizationId = requireNotNull(organization.id)
        return AdminOrganizationResponse.from(
            organization = organization,
            hrCount = userRepository.countByOrganizationIdAndRole(organizationId, UserRole.HR),
            jobCount = jobRepository.countByOrganizationId(organizationId),
        )
    }

    private fun normalizeName(name: String): String {
        return name.trim().ifBlank { throw BadRequestException("Organization name is required") }
    }

    private fun generateToken(): String {
        return buildString {
            append(TOKEN_PREFIX)
            repeat(TOKEN_LENGTH) {
                append(TOKEN_ALPHABET[secureRandom.nextInt(TOKEN_ALPHABET.size)])
            }
        }
    }

    private fun buildTokenPreview(rawToken: String): String {
        return rawToken.take(4) + TOKEN_PREVIEW_MASK + rawToken.takeLast(4)
    }
}