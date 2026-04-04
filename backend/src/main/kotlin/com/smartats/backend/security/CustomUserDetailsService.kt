package com.smartats.backend.security

import com.smartats.backend.repository.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository,
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByUsername(username)
            .orElseThrow { UsernameNotFoundException("User $username not found") }

        return User.builder()
            .username(user.username)
            .password(user.passwordHash)
            .authorities(SimpleGrantedAuthority("ROLE_${user.role.name}"))
            .build()
    }
}