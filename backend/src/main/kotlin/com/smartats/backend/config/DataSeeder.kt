package com.smartats.backend.config

import com.smartats.backend.domain.User
import com.smartats.backend.domain.UserRole
import com.smartats.backend.repository.UserRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
class DataSeeder {

    @Bean
    fun seedAdminUser(userRepository: UserRepository, passwordEncoder: PasswordEncoder): CommandLineRunner {
        return CommandLineRunner {
            if (!userRepository.existsByUsername("admin")) {
                val admin = User(
                    username = "admin",
                    passwordHash = passwordEncoder.encode("admin"),
                    email = "admin@smartats.local",
                    role = UserRole.ADMIN,
                )
                userRepository.save(admin)
                println("Seeded default admin user (admin/admin)")
            }
        }
    }
}
