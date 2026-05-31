package com.smartats.backend.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "admin_parse_failure_review_events",
    indexes = [
        Index(
            name = "idx_admin_parse_failure_review_events_resume_created_at",
            columnList = "resume_id, created_at",
        ),
    ],
)
class AdminParseFailureReviewEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    var resume: Resume,

    @Column(name = "admin_username", nullable = false)
    var adminUsername: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 64)
    var actionType: AdminParseFailureReviewActionType,

    @Column(name = "note", columnDefinition = "TEXT")
    var note: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_review_status", nullable = false, length = 64)
    var previousReviewStatus: AdminParseFailureReviewStatus,

    @Enumerated(EnumType.STRING)
    @Column(name = "next_review_status", nullable = false, length = 64)
    var nextReviewStatus: AdminParseFailureReviewStatus,

    @Column(name = "resume_status_after_action", nullable = false, length = 64)
    var resumeStatusAfterAction: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
) {
    @PrePersist
    fun prePersist() {
        createdAt = LocalDateTime.now()
    }
}