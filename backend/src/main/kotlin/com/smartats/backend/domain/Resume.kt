package com.smartats.backend.domain

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "resumes")
class Resume(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(name = "candidate_name")
    var candidateName: String? = null,

    @Column(name = "contact_info")
    var contactInfo: String? = null,

    @Column(name = "raw_content_reference", nullable = false)
    var rawContentReference: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parsed_data", columnDefinition = "jsonb")
    var parsedData: Map<String, Any>? = null,

    @Column(name = "embedding", columnDefinition = "vector(1536)")
    var embedding: String? = null,

    @Column(name = "parse_failure_reason", columnDefinition = "TEXT")
    var parseFailureReason: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var ownerUser: User? = null,

    @Column(name = "status", nullable = false)
    var status: String = "PENDING_PARSE",

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    @PrePersist
    fun prePersist() {
        val now = LocalDateTime.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = LocalDateTime.now()
    }
}
