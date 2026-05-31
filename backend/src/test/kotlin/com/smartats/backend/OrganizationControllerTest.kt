package com.smartats.backend

import com.smartats.backend.domain.AccessAuditActionType
import com.smartats.backend.domain.AccessAuditActorRole
import com.smartats.backend.domain.AccessAuditTargetType
import com.smartats.backend.repository.AccessAuditEventRepository
import com.smartats.backend.service.AccessAuditService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrganizationControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var accessAuditEventRepository: AccessAuditEventRepository

    @BeforeEach
    fun setUp() {
        accessAuditEventRepository.deleteAll()
    }

    @Test
    fun `public organization list is audited for anonymous access`() {
        mockMvc.perform(get("/api/organizations/public"))
            .andExpect(status().isOk)

        val accessAuditEvent = accessAuditEventRepository.findAll().single()
        assertEquals("anonymousUser", accessAuditEvent.actorUsername)
        assertEquals(AccessAuditActorRole.ANONYMOUS, accessAuditEvent.actorRole)
        assertEquals(AccessAuditActionType.ORGANIZATION_DIRECTORY_VIEWED, accessAuditEvent.actionType)
        assertEquals(AccessAuditTargetType.ORGANIZATION_DIRECTORY, accessAuditEvent.targetType)
        assertEquals(AccessAuditService.ORGANIZATION_DIRECTORY_TARGET_ID, accessAuditEvent.targetId)
    }
}