package com.smartats.backend.task

import com.smartats.backend.config.GovernanceProperties
import com.smartats.backend.repository.AccessAuditEventRepository
import com.smartats.backend.repository.AdminParseFailureReviewEventRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class DataRetentionTask(
    private val governanceProperties: GovernanceProperties,
    private val accessAuditEventRepository: AccessAuditEventRepository,
    private val adminParseFailureReviewEventRepository: AdminParseFailureReviewEventRepository,
) {
    private val logger = LoggerFactory.getLogger(DataRetentionTask::class.java)

    @Scheduled(cron = "\${app.governance.data-retention.purge-cron:0 0 3 * * *}")
    @Transactional
    fun purgeExpiredGovernanceData() {
        if (!governanceProperties.dataRetention.enabled) {
            return
        }

        val now = LocalDateTime.now()
        val accessAuditThreshold = now.minusDays(governanceProperties.dataRetention.accessAuditRetentionDays)
        val parseFailureThreshold = now.minusDays(governanceProperties.dataRetention.parseFailureReviewRetentionDays)

        val deletedAccessAuditEvents = accessAuditEventRepository.deleteByCreatedAtBefore(accessAuditThreshold)
        val deletedParseFailureEvents = adminParseFailureReviewEventRepository.deleteByCreatedAtBefore(parseFailureThreshold)

        if (deletedAccessAuditEvents > 0 || deletedParseFailureEvents > 0) {
            logger.info(
                "Data retention purge completed: deleted {} access audit events and {} parse-failure review events.",
                deletedAccessAuditEvents,
                deletedParseFailureEvents,
            )
        }
    }
}