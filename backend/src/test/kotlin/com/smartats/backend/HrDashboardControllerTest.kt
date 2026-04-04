package com.smartats.backend

import com.fasterxml.jackson.databind.ObjectMapper
import com.smartats.backend.dto.hr.HrDashboardFunnelItem
import com.smartats.backend.dto.hr.HrDashboardKeyMetrics
import com.smartats.backend.dto.hr.HrDashboardSkillDistributionItem
import com.smartats.backend.dto.hr.HrDashboardStatsResponse
import com.smartats.backend.dto.hr.HrDashboardTrends
import com.smartats.backend.service.HrDashboardService
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HrDashboardControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var hrDashboardService: HrDashboardService

    @Test
    fun `hr dashboard stats endpoint returns echarts friendly payload for hr users`() {
        given(hrDashboardService.getStats(30)).willReturn(
            HrDashboardStatsResponse(
                keyMetrics = HrDashboardKeyMetrics(
                    totalResumes = 4821,
                    parsedResumes = 3205,
                    interviewCount = 840,
                    offersSent = 112,
                ),
                funnel = listOf(
                    HrDashboardFunnelItem("收到简历", 4821),
                    HrDashboardFunnelItem("解析入库", 3205),
                    HrDashboardFunnelItem("AI 匹配(A级以上)", 1200),
                    HrDashboardFunnelItem("进入面试", 840),
                    HrDashboardFunnelItem("发出 Offer", 112),
                ),
                skillsDistribution = listOf(
                    HrDashboardSkillDistributionItem("Vue/React", 1048),
                    HrDashboardSkillDistributionItem("Spring Boot", 735),
                ),
                trends = HrDashboardTrends(
                    dates = listOf("03-29", "03-30", "03-31", "04-01", "04-02", "04-03", "04-04"),
                    received = listOf(120, 132, 101, 134, 90, 230, 210),
                    parsed = listOf(90, 100, 80, 110, 70, 190, 180),
                ),
            ),
        )

        mockMvc.perform(
            get("/api/hr/dashboard/stats")
                .param("days", "30")
                .with(user("dashboard_hr").roles("HR")),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.keyMetrics.totalResumes").value(4821))
            .andExpect(jsonPath("$.data.keyMetrics.parsedResumes").value(3205))
            .andExpect(jsonPath("$.data.funnel[2].name").value("AI 匹配(A级以上)"))
            .andExpect(jsonPath("$.data.funnel[2].value").value(1200))
            .andExpect(jsonPath("$.data.skillsDistribution[0].name").value("Vue/React"))
            .andExpect(jsonPath("$.data.trends.dates[6]").value("04-04"))
            .andExpect(jsonPath("$.data.trends.received[5]").value(230))
            .andExpect(jsonPath("$.data.trends.parsed[0]").value(90))

        verify(hrDashboardService).getStats(30)
    }

    @Test
    fun `hr dashboard stats endpoint uses default seven day window when param is omitted`() {
        given(hrDashboardService.getStats(7)).willReturn(
            HrDashboardStatsResponse(
                keyMetrics = HrDashboardKeyMetrics(10, 8, 2, 1),
                funnel = listOf(HrDashboardFunnelItem("收到简历", 10)),
                skillsDistribution = listOf(HrDashboardSkillDistributionItem("Python", 3)),
                trends = HrDashboardTrends(
                    dates = listOf("04-01", "04-02", "04-03", "04-04", "04-05", "04-06", "04-07"),
                    received = listOf(1, 2, 3, 4, 5, 6, 7),
                    parsed = listOf(1, 1, 2, 3, 5, 8, 13),
                ),
            ),
        )

        mockMvc.perform(
            get("/api/hr/dashboard/stats")
                .with(user("dashboard_hr").roles("HR")),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.keyMetrics.totalResumes").value(10))

        verify(hrDashboardService).getStats(7)
    }

    @Test
    fun `hr dashboard stats endpoint rejects candidate users`() {
        mockMvc.perform(
            get("/api/hr/dashboard/stats")
                .with(user("dashboard_candidate").roles("CANDIDATE")),
        )
            .andExpect(status().isForbidden)
    }
}