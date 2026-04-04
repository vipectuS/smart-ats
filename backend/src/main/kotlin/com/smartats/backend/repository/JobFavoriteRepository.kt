package com.smartats.backend.repository

import com.smartats.backend.domain.JobFavorite
import com.smartats.backend.domain.JobFavoriteStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface JobFavoriteRepository : JpaRepository<JobFavorite, UUID> {
    fun findByUserIdAndStatusOrderByUpdatedAtDesc(userId: UUID, status: JobFavoriteStatus): List<JobFavorite>

    fun findByUserIdOrderByUpdatedAtDesc(userId: UUID): List<JobFavorite>

    fun findByUserIdAndJobId(userId: UUID, jobId: UUID): JobFavorite?

    fun existsByUserIdAndJobIdAndStatus(userId: UUID, jobId: UUID, status: JobFavoriteStatus): Boolean
}