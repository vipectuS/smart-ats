package com.smartats.backend.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "organizations")
class Organization(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(nullable = false, unique = true, length = 150)
    var name: String,

    @Column(name = "token_hash", nullable = false, length = 255)
    var tokenHash: String,

    @Column(name = "token_preview", nullable = false, length = 32)
    var tokenPreview: String,

    @Column(nullable = false)
    var enabled: Boolean = true,

    @Column(name = "is_system_default", nullable = false)
    var isSystemDefault: Boolean = false,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant? = null,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null,
) {

    @PrePersist
    fun prePersist() {
        val now = Instant.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = Instant.now()
    }
}