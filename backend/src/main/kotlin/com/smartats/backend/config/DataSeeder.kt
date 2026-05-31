package com.smartats.backend.config

import com.smartats.backend.domain.User
import com.smartats.backend.domain.UserRole
import com.smartats.backend.repository.OrganizationRepository
import com.smartats.backend.repository.UserRepository
import com.smartats.backend.service.OrganizationService
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
class DataSeeder {

    @Bean
    fun seedAdminUser(
        userRepository: UserRepository,
        organizationRepository: OrganizationRepository,
        passwordEncoder: PasswordEncoder,
        adminSeedProperties: AdminSeedProperties,
    ): CommandLineRunner {
        return CommandLineRunner {
            if (!adminSeedProperties.enabled) {
                return@CommandLineRunner
            }

            val defaultOrganization = organizationRepository.findByIsSystemDefaultTrue().orElseGet {
                organizationRepository.save(
                    com.smartats.backend.domain.Organization(
                        id = OrganizationService.SYSTEM_DEFAULT_ORGANIZATION_ID,
                        name = OrganizationService.DEFAULT_ORGANIZATION_NAME,
                        tokenHash = passwordEncoder.encode("org_whoseyards_default_admin_only"),
                        tokenPreview = "sysd****only",
                        enabled = true,
                        isSystemDefault = true,
                    ),
                )
            }

            val admin = userRepository.findByUsername(adminSeedProperties.username).orElseGet {
                User(
                    username = adminSeedProperties.username,
                    passwordHash = passwordEncoder.encode(adminSeedProperties.password),
                    email = adminSeedProperties.email,
                    role = UserRole.ADMIN,
                )
            }

            admin.organization = defaultOrganization
            userRepository.save(admin)
            println("Seeded/updated configured default admin user and default organization binding")
        }
    }
}

