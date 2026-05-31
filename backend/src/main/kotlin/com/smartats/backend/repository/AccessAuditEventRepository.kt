package com.smartats.backend.repository

import com.smartats.backend.domain.AccessAuditActionType
import com.smartats.backend.domain.AccessAuditEvent
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.UUID

interface AccessAuditEventRepository : JpaRepository<AccessAuditEvent, UUID> {
    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): List<AccessAuditEvent>

    fun findByActionTypeOrderByCreatedAtDesc(
        actionType: AccessAuditActionType,
        pageable: Pageable,
    ): List<AccessAuditEvent>

    fun deleteByCreatedAtBefore(createdAt: LocalDateTime): Long
}