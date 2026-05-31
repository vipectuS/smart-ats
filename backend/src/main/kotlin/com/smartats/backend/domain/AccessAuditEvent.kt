package com.smartats.backend.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "access_audit_events",
    indexes = [
        Index(name = "idx_access_audit_events_created_at", columnList = "created_at"),
        Index(name = "idx_access_audit_events_action_type", columnList = "action_type"),
        Index(name = "idx_access_audit_events_target_type_target_id", columnList = "target_type, target_id"),
    ],
)
class AccessAuditEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(name = "actor_username", nullable = false)
    var actorUsername: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_role", nullable = false, length = 32)
    var actorRole: AccessAuditActorRole,

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 64)
    var actionType: AccessAuditActionType,

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 32)
    var targetType: AccessAuditTargetType,

    @Column(name = "target_id", nullable = false)
    var targetId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "sensitive_field", length = 64)
    var sensitiveField: AccessAuditSensitiveField? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
) {
    @PrePersist
    fun prePersist() {
        createdAt = LocalDateTime.now()
    }
}