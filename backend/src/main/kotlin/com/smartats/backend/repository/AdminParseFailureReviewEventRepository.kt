package com.smartats.backend.repository

import com.smartats.backend.domain.AdminParseFailureReviewEvent
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.UUID

interface AdminParseFailureReviewEventRepository : JpaRepository<AdminParseFailureReviewEvent, UUID> {
    fun findByResumeIdOrderByCreatedAtDesc(resumeId: UUID, pageable: Pageable): List<AdminParseFailureReviewEvent>

    fun deleteByCreatedAtBefore(createdAt: LocalDateTime): Long
}