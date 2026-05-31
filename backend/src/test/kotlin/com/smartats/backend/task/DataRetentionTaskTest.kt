package com.smartats.backend.task

import com.smartats.backend.config.GovernanceProperties
import com.smartats.backend.repository.AccessAuditEventRepository
import com.smartats.backend.repository.AdminParseFailureReviewEventRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.Assertions.assertTrue
import java.time.LocalDateTime
import org.mockito.Mock
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class DataRetentionTaskTest {
    @Mock
    private lateinit var accessAuditEventRepository: AccessAuditEventRepository

    @Mock
    private lateinit var adminParseFailureReviewEventRepository: AdminParseFailureReviewEventRepository

    @Test
    fun `skip purge when retention is disabled`() {
        val task = DataRetentionTask(
            governanceProperties = GovernanceProperties(
                dataRetention = GovernanceProperties.DataRetentionProperties(enabled = false),
            ),
            accessAuditEventRepository = accessAuditEventRepository,
            adminParseFailureReviewEventRepository = adminParseFailureReviewEventRepository,
        )

        task.purgeExpiredGovernanceData()

        verifyNoInteractions(accessAuditEventRepository, adminParseFailureReviewEventRepository)
    }

    @Test
    fun `delete expired governance records when retention is enabled`() {
        val task = DataRetentionTask(
            governanceProperties = GovernanceProperties(
                dataRetention = GovernanceProperties.DataRetentionProperties(
                    enabled = true,
                    accessAuditRetentionDays = 180,
                    parseFailureReviewRetentionDays = 90,
                ),
            ),
            accessAuditEventRepository = accessAuditEventRepository,
            adminParseFailureReviewEventRepository = adminParseFailureReviewEventRepository,
        )

        task.purgeExpiredGovernanceData()

        val accessAuditInvocations = mockingDetails(accessAuditEventRepository).invocations
            .filter { it.method.name == "deleteByCreatedAtBefore" }
        val parseFailureInvocations = mockingDetails(adminParseFailureReviewEventRepository).invocations
            .filter { it.method.name == "deleteByCreatedAtBefore" }

        assertEquals(1, accessAuditInvocations.size)
        assertEquals(1, parseFailureInvocations.size)

        val accessAuditThreshold = accessAuditInvocations.single().arguments.single() as LocalDateTime
        val parseFailureThreshold = parseFailureInvocations.single().arguments.single() as LocalDateTime

        assertTrue(accessAuditThreshold.isBefore(LocalDateTime.now().minusDays(179)))
        assertTrue(parseFailureThreshold.isBefore(LocalDateTime.now().minusDays(89)))
    }
}