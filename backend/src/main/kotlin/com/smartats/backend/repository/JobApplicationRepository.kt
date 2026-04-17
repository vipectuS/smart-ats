package com.smartats.backend.repository

import com.smartats.backend.domain.JobApplication
import com.smartats.backend.domain.JobApplicationStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.UUID

interface JobApplicationRepository : JpaRepository<JobApplication, UUID> {
    fun countByStatus(status: JobApplicationStatus): Long

    fun countByStatusAndCreatedAtGreaterThanEqual(status: JobApplicationStatus, createdAt: LocalDateTime): Long

    fun findByJobIdAndStatusOrderByUpdatedAtDesc(jobId: UUID, status: JobApplicationStatus): List<JobApplication>

    fun findByJobIdAndStatusNotOrderByUpdatedAtDesc(jobId: UUID, status: JobApplicationStatus): List<JobApplication>

    fun findByUserIdAndStatusOrderByUpdatedAtDesc(userId: UUID, status: JobApplicationStatus): List<JobApplication>

    fun findByUserIdAndStatusNotOrderByUpdatedAtDesc(userId: UUID, status: JobApplicationStatus): List<JobApplication>

    fun findByUserIdOrderByUpdatedAtDesc(userId: UUID): List<JobApplication>

    fun findByUserIdAndJobId(userId: UUID, jobId: UUID): JobApplication?

    fun findByIdAndJobId(id: UUID, jobId: UUID): JobApplication?

    fun existsByUserIdAndJobIdAndStatus(userId: UUID, jobId: UUID, status: JobApplicationStatus): Boolean
}