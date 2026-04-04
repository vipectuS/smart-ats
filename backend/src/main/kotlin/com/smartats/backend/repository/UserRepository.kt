package com.smartats.backend.repository

import com.smartats.backend.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {
    fun findByUsername(username: String): Optional<User>
    fun existsByUsername(username: String): Boolean
    fun existsByEmail(email: String): Boolean
}