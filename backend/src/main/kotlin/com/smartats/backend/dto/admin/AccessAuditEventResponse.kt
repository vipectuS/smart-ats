package com.smartats.backend.dto.admin

import com.smartats.backend.domain.AccessAuditActionType
import com.smartats.backend.domain.AccessAuditActorRole
import com.smartats.backend.domain.AccessAuditEvent
import com.smartats.backend.domain.AccessAuditSensitiveField
import com.smartats.backend.domain.AccessAuditTargetType
import java.time.LocalDateTime
import java.util.UUID

data class AccessAuditEventResponse(
    val id: UUID,
    val actorUsername: String,
    val actorRole: AccessAuditActorRole,
    val actionType: AccessAuditActionType,
    val targetType: AccessAuditTargetType,
    val targetId: UUID,
    val sensitiveField: AccessAuditSensitiveField?,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(event: AccessAuditEvent): AccessAuditEventResponse {
            return AccessAuditEventResponse(
                id = requireNotNull(event.id),
                actorUsername = event.actorUsername,
                actorRole = event.actorRole,
                actionType = event.actionType,
                targetType = event.targetType,
                targetId = event.targetId,
                sensitiveField = event.sensitiveField,
                createdAt = event.createdAt,
            )
        }
    }
}