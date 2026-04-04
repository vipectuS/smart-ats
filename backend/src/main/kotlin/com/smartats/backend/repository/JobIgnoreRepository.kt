package com.smartats.backend.repository

import com.smartats.backend.domain.JobIgnore
import com.smartats.backend.domain.JobIgnoreStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface JobIgnoreRepository : JpaRepository<JobIgnore, UUID> {
    fun findByUserIdAndStatusOrderByUpdatedAtDesc(userId: UUID, status: JobIgnoreStatus): List<JobIgnore>

    fun findByUserIdOrderByUpdatedAtDesc(userId: UUID): List<JobIgnore>

    fun findByUserIdAndJobId(userId: UUID, jobId: UUID): JobIgnore?

    fun existsByUserIdAndJobIdAndStatus(userId: UUID, jobId: UUID, status: JobIgnoreStatus): Boolean
}