package com.smartats.backend.service

import com.smartats.backend.domain.CandidateProfile
import com.smartats.backend.domain.User
import com.smartats.backend.domain.UserRole
import com.smartats.backend.dto.candidate.CandidateProfileResponse
import com.smartats.backend.dto.candidate.CandidateProfileUpdateRequest
import com.smartats.backend.exception.BadRequestException
import com.smartats.backend.exception.ResourceNotFoundException
import com.smartats.backend.repository.CandidateProfileRepository
import com.smartats.backend.repository.ResumeRepository
import com.smartats.backend.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CandidateProfileService(
    private val userRepository: UserRepository,
    private val candidateProfileRepository: CandidateProfileRepository,
    private val resumeRepository: ResumeRepository,
) {

    @Transactional(readOnly = true)
    fun getCurrentCandidateProfile(username: String): CandidateProfileResponse {
        val user = getCandidateUser(username)
        val profile = candidateProfileRepository.findByUserId(requireNotNull(user.id)).orElse(null)
        val latestResume = resumeRepository.findTopByOwnerUserIdOrderByUpdatedAtDesc(requireNotNull(user.id))
        return CandidateProfileResponse.from(user, profile, latestResume)
    }

    @Transactional
    fun updateCurrentCandidateProfile(
        username: String,
        request: CandidateProfileUpdateRequest,
    ): CandidateProfileResponse {
        val user = getCandidateUser(username)
        val profile = candidateProfileRepository.findByUserId(requireNotNull(user.id))
            .orElseGet { candidateProfileRepository.save(CandidateProfile(user = user)) }

        profile.githubUrl = request.githubUrl?.trim()?.ifBlank { null }
        profile.portfolioUrl = request.portfolioUrl?.trim()?.ifBlank { null }

        val savedProfile = candidateProfileRepository.save(profile)
        val latestResume = resumeRepository.findTopByOwnerUserIdOrderByUpdatedAtDesc(requireNotNull(user.id))
        return CandidateProfileResponse.from(user, savedProfile, latestResume)
    }

    private fun getCandidateUser(username: String): User {
        val user = userRepository.findByUsername(username)
            .orElseThrow { ResourceNotFoundException("User not found") }
        if (user.role != UserRole.CANDIDATE) {
            throw BadRequestException("Current user is not a candidate")
        }
        return user
    }
}