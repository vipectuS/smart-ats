package com.smartats.backend.config

import com.smartats.backend.security.JwtAuthenticationFilter
import com.smartats.backend.security.handler.RestAccessDeniedHandler
import com.smartats.backend.security.handler.RestAuthenticationEntryPoint
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy
import org.springframework.security.config.Customizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val governanceProperties: GovernanceProperties,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val authenticationEntryPoint: RestAuthenticationEntryPoint,
    private val accessDeniedHandler: RestAccessDeniedHandler,
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val securityHeaders = governanceProperties.securityHeaders

        return http
            .csrf { it.disable() }
            .cors { }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .headers(
                Customizer { headers ->
                    if (securityHeaders.enabled) {
                        if (securityHeaders.contentTypeOptionsEnabled) {
                            headers.contentTypeOptions(Customizer.withDefaults())
                        } else {
                            headers.contentTypeOptions { it.disable() }
                        }

                        headers.frameOptions {
                            when (securityHeaders.frameOptionsMode.uppercase()) {
                                "SAMEORIGIN" -> it.sameOrigin()
                                "DISABLE" -> it.disable()
                                else -> it.deny()
                            }
                        }

                        headers.referrerPolicy {
                            it.policy(resolveReferrerPolicy(securityHeaders.referrerPolicy))
                        }

                        if (securityHeaders.hstsEnabled) {
                            headers.httpStrictTransportSecurity {
                                it.includeSubDomains(securityHeaders.includeSubDomains)
                                it.maxAgeInSeconds(securityHeaders.hstsMaxAgeSeconds)
                            }
                        } else {
                            headers.httpStrictTransportSecurity { it.disable() }
                        }
                    } else {
                        headers.defaultsDisabled()
                    }
                },
            )
            .exceptionHandling {
                it.authenticationEntryPoint(authenticationEntryPoint)
                it.accessDeniedHandler(accessDeniedHandler)
            }
            .authorizeHttpRequests {
                it.requestMatchers("/api/v1/auth/**").permitAll()
                it.requestMatchers("/api/auth/**").permitAll()
                it.requestMatchers("/api/organizations/public").permitAll()
                it.requestMatchers("/internal/api/**").permitAll()
                it.requestMatchers("/error").permitAll()
                it.anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()
    }

    private fun resolveReferrerPolicy(policy: String): ReferrerPolicy {
        return when (policy.lowercase()) {
            "no-referrer" -> ReferrerPolicy.NO_REFERRER
            "same-origin" -> ReferrerPolicy.SAME_ORIGIN
            "origin" -> ReferrerPolicy.ORIGIN
            "strict-origin" -> ReferrerPolicy.STRICT_ORIGIN
            "origin-when-cross-origin" -> ReferrerPolicy.ORIGIN_WHEN_CROSS_ORIGIN
            "strict-origin-when-cross-origin" -> ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN
            "unsafe-url" -> ReferrerPolicy.UNSAFE_URL
            else -> ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN
        }
    }
}