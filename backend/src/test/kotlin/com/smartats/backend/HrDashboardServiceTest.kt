package com.smartats.backend

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.smartats.backend.domain.JobApplicationStatus
import com.smartats.backend.domain.Resume
import com.smartats.backend.dto.hr.HrDashboardSkillDistributionItem
import com.smartats.backend.repository.DailyCountProjection
import com.smartats.backend.repository.JobApplicationRepository
import com.smartats.backend.repository.JobRecommendationRepository
import com.smartats.backend.repository.ResumeRepository
import com.smartats.backend.service.HrDashboardService
import com.smartats.backend.service.ResumeService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import java.math.BigDecimal
import java.time.LocalDate

class HrDashboardServiceTest {

    private lateinit var resumeRepository: ResumeRepository
    private lateinit var jobApplicationRepository: JobApplicationRepository
    private lateinit var jobRecommendationRepository: JobRecommendationRepository

    private lateinit var service: HrDashboardService

    @BeforeEach
    fun setUp() {
        resumeRepository = mock(ResumeRepository::class.java)
        jobApplicationRepository = mock(JobApplicationRepository::class.java)
        jobRecommendationRepository = mock(JobRecommendationRepository::class.java)

        service = HrDashboardService(
            resumeRepository = resumeRepository,
            jobApplicationRepository = jobApplicationRepository,
            jobRecommendationRepository = jobRecommendationRepository,
            objectMapper = jacksonObjectMapper(),
        )
    }

    @Test
    fun `getStats returns aggregated metrics funnel skills and trends for requested day window`() {
        val today = LocalDate.now()
        val startDate = today.minusDays(2)
        val startDateTime = startDate.atStartOfDay()

        given(resumeRepository.countByCreatedAtGreaterThanEqual(startDateTime)).willReturn(24)
        given(resumeRepository.countByStatusAndUpdatedAtGreaterThanEqual(ResumeService.STATUS_PARSED, startDateTime)).willReturn(20)
        given(jobRecommendationRepository.countByMatchScoreGreaterThanEqualAndCreatedAtGreaterThanEqual(BigDecimal("80"), startDateTime)).willReturn(8)
        given(jobApplicationRepository.countByStatusAndCreatedAtGreaterThanEqual(JobApplicationStatus.APPLIED, startDateTime)).willReturn(4)
        given(resumeRepository.findByStatusAndUpdatedAtGreaterThanEqual(ResumeService.STATUS_PARSED, startDateTime)).willReturn(
            listOf(
                parsedResumeWithSkills("Kotlin", "Spring Boot", "React"),
                parsedResumeWithSkills("Java", "Python", "Vue"),
                parsedResumeWithSkills("Go", "Product Manager", "Kotlin"),
            ),
        )
        given(resumeRepository.countReceivedByDayBetween(startDate, today)).willReturn(
            listOf(
                projection(startDate, 8),
                projection(startDate.plusDays(2), 12),
                projection(startDate.plusDays(6), 20),
            ),
        )
        given(resumeRepository.countParsedByDayBetween(ResumeService.STATUS_PARSED, startDate, today)).willReturn(
            listOf(
                projection(startDate, 5),
                projection(startDate.plusDays(1), 9),
                projection(startDate.plusDays(2), 18),
            ),
        )

        val stats = service.getStats(3)

        assertEquals(24, stats.keyMetrics.totalResumes)
        assertEquals(20, stats.keyMetrics.parsedResumes)
        assertEquals(4, stats.keyMetrics.interviewCount)
        assertEquals(1, stats.keyMetrics.offersSent)

        assertEquals(
            listOf(24L, 20L, 8L, 4L, 1L),
            stats.funnel.map { it.value },
        )
        assertEquals(
            listOf("Spring Boot", "Vue/React", "Go", "Python", "管理/产品"),
            stats.skillsDistribution.map { it.name },
        )
        assertEquals(listOf(4L, 2L, 1L, 1L, 1L), stats.skillsDistribution.map { it.value })
        assertEquals(3, stats.trends.dates.size)
        assertEquals(listOf(8L, 0L, 12L), stats.trends.received)
        assertEquals(listOf(5L, 9L, 18L), stats.trends.parsed)
    }

    @Test
    fun `getStats falls back to mock skills distribution when parsed skills are unavailable`() {
        val today = LocalDate.now()
        val startDate = today
        val startDateTime = startDate.atStartOfDay()

        given(resumeRepository.countByCreatedAtGreaterThanEqual(startDateTime)).willReturn(0)
        given(resumeRepository.countByStatusAndUpdatedAtGreaterThanEqual(ResumeService.STATUS_PARSED, startDateTime)).willReturn(0)
        given(jobRecommendationRepository.countByMatchScoreGreaterThanEqualAndCreatedAtGreaterThanEqual(BigDecimal("80"), startDateTime)).willReturn(0)
        given(jobApplicationRepository.countByStatusAndCreatedAtGreaterThanEqual(JobApplicationStatus.APPLIED, startDateTime)).willReturn(0)
        given(resumeRepository.findByStatusAndUpdatedAtGreaterThanEqual(ResumeService.STATUS_PARSED, startDateTime)).willReturn(emptyList())
        given(resumeRepository.countReceivedByDayBetween(startDate, today)).willReturn(emptyList())
        given(resumeRepository.countParsedByDayBetween(ResumeService.STATUS_PARSED, startDate, today)).willReturn(emptyList())

        val stats = service.getStats(1)

        assertEquals(
            listOf(
                HrDashboardSkillDistributionItem(name = "Vue/React", value = 1048),
                HrDashboardSkillDistributionItem(name = "Spring Boot", value = 735),
                HrDashboardSkillDistributionItem(name = "Python", value = 580),
                HrDashboardSkillDistributionItem(name = "Go", value = 484),
                HrDashboardSkillDistributionItem(name = "管理/产品", value = 300),
            ),
            stats.skillsDistribution,
        )
        assertEquals(listOf(0L), stats.trends.received)
        assertEquals(listOf(0L), stats.trends.parsed)
    }

    @Test
    fun `getStats coerces invalid days to a one day window`() {
        val today = LocalDate.now()
        val startDateTime = today.atStartOfDay()

        given(resumeRepository.countByCreatedAtGreaterThanEqual(startDateTime)).willReturn(3)
        given(resumeRepository.countByStatusAndUpdatedAtGreaterThanEqual(ResumeService.STATUS_PARSED, startDateTime)).willReturn(2)
        given(jobRecommendationRepository.countByMatchScoreGreaterThanEqualAndCreatedAtGreaterThanEqual(BigDecimal("80"), startDateTime)).willReturn(1)
        given(jobApplicationRepository.countByStatusAndCreatedAtGreaterThanEqual(JobApplicationStatus.APPLIED, startDateTime)).willReturn(1)
        given(resumeRepository.findByStatusAndUpdatedAtGreaterThanEqual(ResumeService.STATUS_PARSED, startDateTime)).willReturn(emptyList())
        given(resumeRepository.countReceivedByDayBetween(today, today)).willReturn(emptyList())
        given(resumeRepository.countParsedByDayBetween(ResumeService.STATUS_PARSED, today, today)).willReturn(emptyList())

        val stats = service.getStats(0)

        assertEquals(1, stats.trends.dates.size)
        assertEquals(3, stats.keyMetrics.totalResumes)
    }

    private fun parsedResumeWithSkills(vararg skills: String): Resume {
        return Resume(
            rawContentReference = "s3://resumes/mock.pdf",
            parsedData = mapOf(
                "skills" to skills.map {
                    mapOf(
                        "name" to it,
                        "category" to "technical",
                    )
                },
            ),
            status = ResumeService.STATUS_PARSED,
        )
    }

    private fun projection(day: LocalDate, total: Long): DailyCountProjection {
        return object : DailyCountProjection {
            override fun getDay(): LocalDate = day

            override fun getTotal(): Long = total
        }
    }
}