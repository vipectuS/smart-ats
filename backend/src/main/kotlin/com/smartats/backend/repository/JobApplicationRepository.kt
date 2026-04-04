package com.smartats.backend.repository

import com.smartats.backend.domain.JobApplication
import com.smartats.backend.domain.JobApplicationStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.UUID

interface JobApplicationRepository : JpaRepository<JobApplication, UUID> {
    fun countByStatus(status: JobApplicationStatus): Long

    fun countByStatusAndCreatedAtGreaterThanEqual(status: JobApplicationStatus, createdAt: LocalDateTime): Long

    fun findByUserIdAndStatusOrderByUpdatedAtDesc(userId: UUID, status: JobApplicationStatus): List<JobApplication>

    fun findByUserIdOrderByUpdatedAtDesc(userId: UUID): List<JobApplication>

    fun findByUserIdAndJobId(userId: UUID, jobId: UUID): JobApplication?

    fun existsByUserIdAndJobIdAndStatus(userId: UUID, jobId: UUID, status: JobApplicationStatus): Boolean
}