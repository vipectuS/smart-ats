package com.smartats.backend.dto

import com.fasterxml.jackson.databind.ObjectMapper
import com.smartats.backend.dto.talent.TalentProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class TalentProfileMappingTest {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `talent profile maps ai service payload into strong types`() {
        val payload = mapOf(
            "basicInfo" to mapOf(
                "fullName" to "Jane Doe",
                "email" to "jane@example.com",
                "headline" to "Senior Kotlin Engineer",
            ),
            "workExperiences" to listOf(
                mapOf(
                    "company" to "Acme",
                    "title" to "Backend Engineer",
                    "startDate" to "2022-01",
                    "endDate" to "2024-12",
                    "responsibilities" to listOf("Built APIs"),
                    "achievements" to listOf("Reduced latency by 35%"),
                ),
            ),
            "educationExperiences" to listOf(
                mapOf(
                    "school" to "Tech University",
                    "degree" to "BSc",
                    "fieldOfStudy" to "Computer Science",
                ),
            ),
            "skills" to listOf(
                mapOf(
                    "name" to "Kotlin",
                    "category" to "Programming Language",
                    "proficiency" to "Advanced",
                ),
            ),
            "radarScores" to mapOf(
                "communication" to 88,
                "technicalDepth" to 92,
                "problemSolving" to 95,
                "collaboration" to 84,
                "leadership" to 76,
                "adaptability" to 90,
            ),
            "xaiReasoning" to "Candidate aligns strongly with backend platform work",
        )

        val profile = objectMapper.convertValue(payload, TalentProfile::class.java)

        assertEquals("Jane Doe", profile.basicInfo.fullName)
        assertEquals("Backend Engineer", profile.workExperiences.first().title)
        assertEquals("Computer Science", profile.educationExperiences.first().fieldOfStudy)
        assertEquals("Kotlin", profile.skills.first().name)
        assertEquals(92, profile.radarScores.technicalDepth)
        assertEquals("Candidate aligns strongly with backend platform work", profile.xaiReasoning)
    }
}