package com.smartats.backend.repository

import com.smartats.backend.domain.Organization
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface OrganizationRepository : JpaRepository<Organization, UUID> {
    fun findAllByEnabledTrueOrderByNameAsc(): List<Organization>
    fun findAllByOrderByNameAsc(): List<Organization>
    fun findByIsSystemDefaultTrue(): Optional<Organization>
    fun findByNameIgnoreCase(name: String): Optional<Organization>
    fun existsByNameIgnoreCase(name: String): Boolean
    fun existsByIdNotAndNameIgnoreCase(id: UUID, name: String): Boolean
}