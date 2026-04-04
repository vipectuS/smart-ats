package com.smartats.backend.repository

import com.smartats.backend.domain.JobRecommendation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface JobRecommendationRepository : JpaRepository<JobRecommendation, UUID> {
    fun countByMatchScoreGreaterThanEqual(matchScore: BigDecimal): Long

    fun countByMatchScoreGreaterThanEqualAndCreatedAtGreaterThanEqual(matchScore: BigDecimal, createdAt: LocalDateTime): Long

    fun findByJobId(jobId: UUID): List<JobRecommendation>
    fun findByResumeId(resumeId: UUID): List<JobRecommendation>
    fun deleteByJobId(jobId: UUID)
}