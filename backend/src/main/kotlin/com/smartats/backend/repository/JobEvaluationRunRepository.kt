package com.smartats.backend.repository

import com.smartats.backend.domain.JobEvaluationRun
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface JobEvaluationRunRepository : JpaRepository<JobEvaluationRun, UUID> {
    fun findTopByJobIdOrderByVersionNumberDesc(jobId: UUID): JobEvaluationRun?
    fun findByJobIdOrderByVersionNumberDesc(jobId: UUID, pageable: Pageable): List<JobEvaluationRun>
}