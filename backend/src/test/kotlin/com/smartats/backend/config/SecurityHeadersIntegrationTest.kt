package com.smartats.backend.config

import com.smartats.backend.SmartAtsBackendApplication
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootTest(
    classes = [SmartAtsBackendApplication::class, SecurityHeadersIntegrationTest.TestController::class],
    properties = [
        "app.governance.security-headers.enabled=true",
        "app.governance.security-headers.hsts-enabled=true",
        "app.governance.security-headers.frame-options-mode=SAMEORIGIN",
        "app.governance.security-headers.referrer-policy=same-origin",
    ],
)
@AutoConfigureMockMvc
@Import(SecurityHeadersIntegrationTest.TestController::class)
class SecurityHeadersIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    @WithMockUser(username = "tester", roles = ["ADMIN"])
    fun `authenticated response includes configured security headers`() {
        mockMvc.get("/test-security-headers") {
            accept = MediaType.TEXT_PLAIN
            secure = true
        }
            .andExpect {
                status { isOk() }
                header { string("X-Content-Type-Options", "nosniff") }
                header { string("X-Frame-Options", "SAMEORIGIN") }
                header { string("Referrer-Policy", "same-origin") }
                header { string("Strict-Transport-Security", "max-age=31536000 ; includeSubDomains") }
            }
    }

    @RestController
    @RequestMapping("/test-security-headers")
    class TestController {
        @GetMapping
        fun read(): String = "ok"
    }
}