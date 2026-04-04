package com.smartats.backend.repository

import com.smartats.backend.domain.CandidateProfile
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface CandidateProfileRepository : JpaRepository<CandidateProfile, UUID> {
    fun findByUserId(userId: UUID): Optional<CandidateProfile>
}