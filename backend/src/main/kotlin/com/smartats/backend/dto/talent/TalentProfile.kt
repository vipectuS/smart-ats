package com.smartats.backend.dto.talent

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class TalentProfile(
    @JsonProperty("basicInfo")
    val basicInfo: BasicInfo,

    @JsonProperty("workExperiences")
    val workExperiences: List<WorkExperience> = emptyList(),

    @JsonProperty("educationExperiences")
    val educationExperiences: List<EducationExperience> = emptyList(),

    val skills: List<Skill> = emptyList(),

    @JsonProperty("radarScores")
    val radarScores: RadarScores,

    @JsonProperty("xaiReasoning")
    val xaiReasoning: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BasicInfo(
    @JsonProperty("fullName")
    val fullName: String,
    val email: String? = null,
    val phone: String? = null,
    val location: String? = null,
    val headline: String? = null,
    val summary: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WorkExperience(
    val company: String,
    val title: String,
    @JsonProperty("startDate")
    val startDate: String,
    @JsonProperty("endDate")
    val endDate: String? = null,
    val responsibilities: List<String> = emptyList(),
    val achievements: List<String> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EducationExperience(
    val school: String,
    val degree: String,
    @JsonProperty("fieldOfStudy")
    val fieldOfStudy: String? = null,
    @JsonProperty("startDate")
    val startDate: String? = null,
    @JsonProperty("endDate")
    val endDate: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Skill(
    val name: String,
    val category: String? = null,
    val proficiency: String? = null,
    val evidence: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RadarScores(
    val communication: Int,
    @JsonProperty("technicalDepth")
    val technicalDepth: Int,
    @JsonProperty("problemSolving")
    val problemSolving: Int,
    val collaboration: Int,
    val leadership: Int,
    val adaptability: Int,
)