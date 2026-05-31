package com.smartats.backend.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "job_evaluation_runs",
    indexes = [
        Index(name = "idx_job_evaluation_runs_job_version", columnList = "job_id, version_number"),
    ],
)
class JobEvaluationRun(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    var job: Job,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluated_by_user_id")
    var evaluatedBy: User? = null,

    @Column(name = "version_number", nullable = false)
    var versionNumber: Int,

    @Column(name = "skill_weight", nullable = false, precision = 5, scale = 2)
    var skillWeight: BigDecimal,

    @Column(name = "experience_weight", nullable = false, precision = 5, scale = 2)
    var experienceWeight: BigDecimal,

    @Column(name = "education_weight", nullable = false, precision = 5, scale = 2)
    var educationWeight: BigDecimal,

    @Column(name = "semantic_weight", nullable = false, precision = 5, scale = 2)
    var semanticWeight: BigDecimal,

    @Column(name = "evaluated_count", nullable = false)
    var evaluatedCount: Int,

    @Column(name = "evaluation_note", columnDefinition = "TEXT")
    var evaluationNote: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recommendation_snapshot", nullable = false, columnDefinition = "jsonb")
    var recommendationSnapshot: List<Map<String, Any>> = emptyList(),

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
) {

    @PrePersist
    fun prePersist() {
        createdAt = LocalDateTime.now()
    }
}