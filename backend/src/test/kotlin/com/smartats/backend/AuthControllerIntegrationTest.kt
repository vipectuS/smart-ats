package com.smartats.backend

import com.fasterxml.jackson.databind.ObjectMapper
import com.smartats.backend.domain.User
import com.smartats.backend.domain.UserRole
import com.smartats.backend.dto.auth.LoginRequest
import com.smartats.backend.dto.auth.RegisterRequest
import com.smartats.backend.repository.JobRecommendationRepository
import com.smartats.backend.repository.JobRepository
import com.smartats.backend.repository.ResumeRepository
import com.smartats.backend.repository.UserRepository
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var resumeRepository: ResumeRepository

    @Autowired
    private lateinit var jobRepository: JobRepository

    @Autowired
    private lateinit var jobRecommendationRepository: JobRecommendationRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @BeforeEach
    fun setUp() {
        jobRecommendationRepository.deleteAll()
        jobRepository.deleteAll()
        resumeRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `register creates user successfully`() {
        val request = RegisterRequest(
            username = "candidate_one",
            email = "candidate@example.com",
            password = "Password123",
            role = UserRole.CANDIDATE,
        )

        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value(201))
            .andExpect(jsonPath("$.data.username").value("candidate_one"))
            .andExpect(jsonPath("$.data.email").value("candidate@example.com"))
            .andExpect(jsonPath("$.data.role").value("CANDIDATE"))
    }

    @Test
    fun `register rejects public admin registration`() {
        val request = RegisterRequest(
            username = "bad_admin",
            email = "bad_admin@example.com",
            password = "Password123",
            role = UserRole.ADMIN,
        )

        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Public registration cannot assign ADMIN role"))
    }

    @Test
    fun `register rejects duplicate username`() {
        userRepository.save(
            User(
                username = "duplicate_user",
                passwordHash = passwordEncoder.encode("Password123"),
                email = "original@example.com",
                role = UserRole.HR,
            ),
        )

        val request = RegisterRequest(
            username = "duplicate_user",
            email = "new@example.com",
            password = "Password123",
            role = UserRole.HR,
        )

        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("Username already exists"))
    }

    @Test
    fun `login returns jwt for valid credentials`() {
        userRepository.save(
            User(
                username = "security_user",
                passwordHash = passwordEncoder.encode("Password123"),
                email = "security@example.com",
                role = UserRole.ADMIN,
            ),
        )

        val request = LoginRequest(username = "security_user", password = "Password123")

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.accessToken", not("")))
                .andExpect(jsonPath("$.data.user.role").value("ADMIN"))
    }

    @Test
    fun `login rejects invalid credentials`() {
        userRepository.save(
            User(
                username = "wrong_password_user",
                passwordHash = passwordEncoder.encode("Password123"),
                email = "wrong@example.com",
                role = UserRole.HR,
            ),
        )

        val request = LoginRequest(username = "wrong_password_user", password = "BadPassword1")

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("Invalid username or password"))
    }

    @Test
    fun `me endpoint returns current user with bearer token`() {
        userRepository.save(
            User(
                username = "current_user",
                passwordHash = passwordEncoder.encode("Password123"),
                email = "current@example.com",
                role = UserRole.HR,
            ),
        )

        val loginRequest = LoginRequest(username = "current_user", password = "Password123")
        val loginResult = mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)),
        )
            .andExpect(status().isOk)
            .andReturn()

        val accessToken = objectMapper.readTree(loginResult.response.contentAsString)
            .path("data")
            .path("accessToken")
            .asText()

        mockMvc.perform(
            get("/api/v1/users/me")
                .header("Authorization", "Bearer $accessToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.username").value("current_user"))
            .andExpect(jsonPath("$.data.email").value("current@example.com"))
    }
}