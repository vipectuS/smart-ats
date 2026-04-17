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
    fun seedAdminUser(
        userRepository: UserRepository,
        passwordEncoder: PasswordEncoder,
        adminSeedProperties: AdminSeedProperties,
    ): CommandLineRunner {
        return CommandLineRunner {
            if (!adminSeedProperties.enabled) {
                return@CommandLineRunner
            }

            if (!userRepository.existsByUsername(adminSeedProperties.username)) {
                val admin = User(
                    username = adminSeedProperties.username,
                    passwordHash = passwordEncoder.encode(adminSeedProperties.password),
                    email = adminSeedProperties.email,
                    role = UserRole.ADMIN,
                )
                userRepository.save(admin)
                println("Seeded configured default admin user")
            }
        }
    }
}

