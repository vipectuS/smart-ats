package com.smartats.backend.dto.job

import jakarta.validation.constraints.DecimalMin
import java.math.BigDecimal
import java.math.RoundingMode

data class EvaluationRequestWeightsDTO(
    @field:DecimalMin(value = "0.0", inclusive = true)
    val skillWeight: BigDecimal? = null,
    @field:DecimalMin(value = "0.0", inclusive = true)
    val experienceWeight: BigDecimal? = null,
    @field:DecimalMin(value = "0.0", inclusive = true)
    val educationWeight: BigDecimal? = null,
    @field:DecimalMin(value = "0.0", inclusive = true)
    val semanticWeight: BigDecimal? = null,
)

data class AppliedEvaluationWeights(
    val skillWeight: BigDecimal,
    val experienceWeight: BigDecimal,
    val educationWeight: BigDecimal,
    val semanticWeight: BigDecimal,
) {
    companion object {
        fun fromRequest(request: EvaluationRequestWeightsDTO?): AppliedEvaluationWeights {
            if (request == null) {
                return AppliedEvaluationWeights(
                    skillWeight = BigDecimal("35.00"),
                    experienceWeight = BigDecimal("25.00"),
                    educationWeight = BigDecimal("10.00"),
                    semanticWeight = BigDecimal("30.00"),
                )
            }

            val rawSkill = request.skillWeight ?: BigDecimal.ZERO
            val rawExperience = request.experienceWeight ?: BigDecimal.ZERO
            val rawEducation = request.educationWeight ?: BigDecimal.ZERO
            val rawSemantic = request.semanticWeight ?: BigDecimal.ZERO
            val total = rawSkill + rawExperience + rawEducation + rawSemantic
            require(total > BigDecimal.ZERO) { "At least one evaluation weight must be greater than zero" }

            return AppliedEvaluationWeights(
                skillWeight = rawSkill.divide(total, 4, RoundingMode.HALF_UP).multiply(BigDecimal(100)).setScale(2, RoundingMode.HALF_UP),
                experienceWeight = rawExperience.divide(total, 4, RoundingMode.HALF_UP).multiply(BigDecimal(100)).setScale(2, RoundingMode.HALF_UP),
                educationWeight = rawEducation.divide(total, 4, RoundingMode.HALF_UP).multiply(BigDecimal(100)).setScale(2, RoundingMode.HALF_UP),
                semanticWeight = rawSemantic.divide(total, 4, RoundingMode.HALF_UP).multiply(BigDecimal(100)).setScale(2, RoundingMode.HALF_UP),
            )
        }
    }
}