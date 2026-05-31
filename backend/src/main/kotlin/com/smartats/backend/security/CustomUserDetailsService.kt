package com.smartats.backend.security

import com.smartats.backend.repository.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import java.util.LinkedHashSet

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository,
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByUsername(username)
            .orElseThrow { UsernameNotFoundException("User $username not found") }

        val authorities = LinkedHashSet<SimpleGrantedAuthority>()
        authorities.add(SimpleGrantedAuthority("ROLE_${user.role.name}"))
        // ADMIN users should also pass HR-gated endpoints and flows.
        if (user.role.name == "ADMIN") {
            authorities.add(SimpleGrantedAuthority("ROLE_HR"))
        }

        return User.builder()
            .username(user.username)
            .password(user.passwordHash)
            .authorities(authorities)
            .build()
    }
}