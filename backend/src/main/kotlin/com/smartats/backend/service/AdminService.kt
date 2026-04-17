package com.smartats.backend.service

import com.smartats.backend.domain.SkillDictionaryEntry
import com.smartats.backend.domain.UserRole
import com.smartats.backend.dto.admin.AdminDistributionItemResponse
import com.smartats.backend.dto.admin.AdminOverviewResponse
import com.smartats.backend.dto.admin.AdminOverviewTotalsResponse
import com.smartats.backend.dto.admin.AdminParseFailureResponse
import com.smartats.backend.dto.admin.AdminSkillResponse
import com.smartats.backend.dto.admin.AdminSkillUpsertRequest
import com.smartats.backend.exception.BadRequestException
import com.smartats.backend.exception.DuplicateResourceException
import com.smartats.backend.exception.ResourceNotFoundException
import com.smartats.backend.repository.JobRepository
import com.smartats.backend.repository.ResumeRepository
import com.smartats.backend.repository.SkillDictionaryRepository
import com.smartats.backend.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AdminService(
    private val userRepository: UserRepository,
    private val jobRepository: JobRepository,
    private val resumeRepository: ResumeRepository,
    private val skillDictionaryRepository: SkillDictionaryRepository,
) {

    @Transactional(readOnly = true)
    fun getOverview(): AdminOverviewResponse {
        return AdminOverviewResponse(
            totals = AdminOverviewTotalsResponse(
                totalUsers = userRepository.count(),
                totalJobs = jobRepository.count(),
                totalResumes = resumeRepository.count(),
                totalSkillEntries = skillDictionaryRepository.count(),
            ),
            usersByRole = UserRole.entries.map { role ->
                AdminDistributionItemResponse(label = role.name, value = userRepository.countByRole(role))
            },
            resumesByStatus = listOf(
                ResumeService.STATUS_PENDING_PARSE,
                ResumeService.STATUS_PARSING,
                ResumeService.STATUS_PARSED,
                ResumeService.STATUS_PARSE_FAILED,
            ).map { status ->
                AdminDistributionItemResponse(label = status, value = resumeRepository.countByStatus(status))
            },
            latestParseFailures = resumeRepository.findByStatusOrderByUpdatedAtDesc(
                ResumeService.STATUS_PARSE_FAILED,
                PageRequest.of(0, 5),
            ).map { resume ->
                AdminParseFailureResponse(
                    resumeId = requireNotNull(resume.id),
                    ownerUsername = resume.ownerUser?.username,
                    sourceFileName = resume.browserPreprocessedPayload?.get("sourceFileName") as? String,
                    rawContentReference = resume.rawContentReference,
                    reason = resume.parseFailureReason,
                    updatedAt = resume.updatedAt,
                )
            },
        )
    }

    @Transactional(readOnly = true)
    fun listSkills(): List<AdminSkillResponse> {
        return skillDictionaryRepository.findAllByOrderByEnabledDescNameAsc()
            .map(AdminSkillResponse::from)
    }

    @Transactional
    fun createSkill(request: AdminSkillUpsertRequest): AdminSkillResponse {
        val normalizedName = normalizeName(request.name)
        if (skillDictionaryRepository.existsByNameIgnoreCase(normalizedName)) {
            throw DuplicateResourceException("Skill already exists")
        }

        val entry = skillDictionaryRepository.save(
            SkillDictionaryEntry(
                name = normalizedName,
                category = normalizeOptionalText(request.category),
                aliases = normalizeAliases(request.aliases, normalizedName),
                enabled = request.enabled,
            ),
        )
        return AdminSkillResponse.from(entry)
    }

    @Transactional
    fun updateSkill(skillId: UUID, request: AdminSkillUpsertRequest): AdminSkillResponse {
        val entry = skillDictionaryRepository.findById(skillId)
            .orElseThrow { ResourceNotFoundException("Skill entry not found") }
        val normalizedName = normalizeName(request.name)

        if (skillDictionaryRepository.existsByIdNotAndNameIgnoreCase(skillId, normalizedName)) {
            throw DuplicateResourceException("Skill already exists")
        }

        entry.name = normalizedName
        entry.category = normalizeOptionalText(request.category)
        entry.aliases = normalizeAliases(request.aliases, normalizedName)
        entry.enabled = request.enabled
        return AdminSkillResponse.from(skillDictionaryRepository.save(entry))
    }

    @Transactional(readOnly = true)
    fun listParseFailures(limit: Int): List<AdminParseFailureResponse> {
        val normalizedLimit = limit.coerceIn(1, 100)
        return resumeRepository.findByStatusOrderByUpdatedAtDesc(
            ResumeService.STATUS_PARSE_FAILED,
            PageRequest.of(0, normalizedLimit),
        ).map { resume ->
            AdminParseFailureResponse(
                resumeId = requireNotNull(resume.id),
                ownerUsername = resume.ownerUser?.username,
                sourceFileName = resume.browserPreprocessedPayload?.get("sourceFileName") as? String,
                rawContentReference = resume.rawContentReference,
                reason = resume.parseFailureReason,
                updatedAt = resume.updatedAt,
            )
        }
    }

    private fun normalizeName(value: String): String {
        return value.trim().ifBlank { throw BadRequestException("Skill name is required") }
    }

    private fun normalizeOptionalText(value: String?): String? {
        return value?.trim()?.ifBlank { null }
    }

    private fun normalizeAliases(aliases: List<String>, normalizedName: String): List<String> {
        return aliases
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filterNot { it.equals(normalizedName, ignoreCase = true) }
            .distinctBy { it.lowercase() }
    }
}