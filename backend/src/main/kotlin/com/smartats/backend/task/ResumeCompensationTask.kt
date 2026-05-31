package com.smartats.backend.task

import com.smartats.backend.dto.resume.composeParseFailureValue
import com.smartats.backend.repository.ResumeRepository
import com.smartats.backend.service.ResumeService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 为了解决 THESIS_IMPROVEMENT_PLAN.md 中的问题三：
 * 防止 AI 解析服务（ai-service）遭遇 OOM、大模型接口超时等各种情况，导致消息队列消息丢失而使简历永远卡在 "PARSING" 或 "PENDING_PARSE" 的状态。
 * 定时补偿任务每隔 15 分钟扫描一次超过 30 分钟没有变动更新的解析中简历，并将其修改为状态 "PARSE_FAILED" 以便抛出失败并兜底处理。
 */
@Component
class ResumeCompensationTask(
    private val resumeRepository: ResumeRepository
) {
    private val logger = LoggerFactory.getLogger(ResumeCompensationTask::class.java)

    // 每 15 分钟执行一次 (15 * 60 * 1000 = 900000 毫秒)
    @Scheduled(fixedRate = 900000)
    @Transactional
    fun recoverStuckResumes() {
        val staledThreshold = LocalDateTime.now().minusMinutes(30)
        
        val parsingStuck = resumeRepository.findByStatusAndUpdatedAtBefore(ResumeService.STATUS_PARSING, staledThreshold)
        val pendingStuck = resumeRepository.findByStatusAndUpdatedAtBefore(ResumeService.STATUS_PENDING_PARSE, staledThreshold)
        
        val allStuckResumes = parsingStuck + pendingStuck

        if (allStuckResumes.isNotEmpty()) {
            logger.warn("检测到 {} 份简历长期卡在解析或者挂起状态未得到响应，触发系统兜底策略设置其为 PARSE_FAILED", allStuckResumes.size)
            
            allStuckResumes.forEach { resume ->
                resume.status = ResumeService.STATUS_PARSE_FAILED
                resume.parseFailureReason = composeParseFailureValue(
                    "RESUME_PARSE_TIMEOUT",
                    "解析服务响应超时或发生异常断联 (Timeout / Null Response)，由系统补偿任务兜底判为失败",
                )
            }
            
            // Hibernate 会通过 dirty checking 自动发出 update 并修改 updated_at 分隔时间点
            resumeRepository.saveAll(allStuckResumes)
            logger.info("系统兜底策略执行完毕：已将 {} 份简历置为解析失败。", allStuckResumes.size)
        }
    }
}
