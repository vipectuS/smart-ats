package com.smartats.backend.service

import com.smartats.backend.dto.auth.UserResponse
import com.smartats.backend.exception.ResourceNotFoundException
import com.smartats.backend.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
) {

    @Transactional(readOnly = true)
    fun getCurrentUser(username: String): UserResponse {
        val user = userRepository.findByUsername(username)
            .orElseThrow { ResourceNotFoundException("User not found") }
        return UserResponse.from(user)
    }
}