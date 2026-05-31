package com.smartats.backend.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.smartats.backend.domain.Job
import com.smartats.backend.domain.JobEvaluationRun
import com.smartats.backend.domain.JobRecommendation
import com.smartats.backend.domain.Resume
import com.smartats.backend.domain.User
import com.smartats.backend.domain.UserRole
import com.smartats.backend.dto.candidate.CandidateJobMatchResponse
import com.smartats.backend.dto.candidate.CandidateJobRecommendationResponse
import com.smartats.backend.dto.candidate.JobActionStateResponse
import com.smartats.backend.dto.job.AppliedEvaluationWeights
import com.smartats.backend.dto.job.EvaluationRequestWeightsDTO
import com.smartats.backend.dto.job.JobEvaluationDeltaSummary
import com.smartats.backend.dto.job.JobEvaluationRecommendationSnapshot
import com.smartats.backend.dto.job.JobEvaluationResponse
import com.smartats.backend.dto.job.JobEvaluationTopCandidateChange
import com.smartats.backend.dto.job.JobEvaluationVersionResponse
import com.smartats.backend.dto.job.JobEvaluationWeightDelta
import com.smartats.backend.dto.job.JobRecommendationCandidateResponse
import com.smartats.backend.dto.job.JobRecommendationScoreBreakdown
import com.smartats.backend.dto.job.JobRecommendationResponse
import com.smartats.backend.dto.organization.OrganizationRefResponse
import com.smartats.backend.dto.talent.TalentProfile
import com.smartats.backend.dto.xai.JobFitReportRequest
import com.smartats.backend.dto.xai.StructuredJobFitReport
import com.smartats.backend.exception.ApiErrorCode
import com.smartats.backend.exception.BadRequestException
import com.smartats.backend.exception.ResourceNotFoundException
import com.smartats.backend.repository.JobEvaluationRunRepository
import com.smartats.backend.repository.JobRecommendationRepository
import com.smartats.backend.repository.JobRepository
import com.smartats.backend.repository.ResumeRepository
import com.smartats.backend.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.util.UUID

@Service
class RecommendationService(
    private val jobRepository: JobRepository,
    private val resumeRepository: ResumeRepository,
    private val jobRecommendationRepository: JobRecommendationRepository,
    private val jobEvaluationRunRepository: JobEvaluationRunRepository,
    private val userRepository: UserRepository,
    private val embeddingService: EmbeddingService,
    private val jobFitReportService: JobFitReportService,
    private val candidateJobActionService: CandidateJobActionService,
    private val objectMapper: ObjectMapper,
    private val skillDictionaryRepository: com.smartats.backend.repository.SkillDictionaryRepository,
) {

    companion object {
        private val TOKEN_SPLIT_REGEX = Regex("[^a-z0-9+#.]+")
        private val CJK_TEXT_REGEX = Regex("[\\u4E00-\\u9FFF]")
        private val STOP_WORDS = setOf(
            "a", "an", "and", "are", "as", "at", "be", "build", "by", "for", "from", "in", "into",
            "is", "of", "on", "or", "that", "the", "to", "with", "we", "you", "our", "will", "can",
        )
    }

    private data class ScoredRecommendation(
        val job: Job,
        val resume: Resume,
        val profile: TalentProfile,
        val skillScore: BigDecimal,
        val experienceScore: BigDecimal,
        val educationScore: BigDecimal,
        val semanticScore: BigDecimal,
        val cosineDistance: BigDecimal,
        val matchScore: BigDecimal,
        val matchedSkills: Set<String>,
        val missingSkills: Set<String>,
        val xaiReasoning: String,
        val suitabilityReport: String,
        val xaiReport: StructuredJobFitReport,
    )

    private data class SkillScoreBreakdown(
        val matchedSkills: Set<String>,
        val missingSkills: Set<String>,
        val keywordHits: Set<String>,
        val score: BigDecimal,
    )

    private data class ExperienceScoreBreakdown(
        val candidateYears: BigDecimal,
        val requiredYears: BigDecimal?,
        val matchedKeywords: Set<String>,
        val score: BigDecimal,
    )

    private data class EducationScoreBreakdown(
        val matchedKeywords: Set<String>,
        val hasEducation: Boolean,
        val score: BigDecimal,
    )

    @Transactional(readOnly = true)
    fun parseTalentProfile(resumeId: UUID): TalentProfile {
        val resume = resumeRepository.findById(resumeId)
            .orElseThrow { ResourceNotFoundException("Resume not found") }

        val parsedData = resume.parsedData
            ?: throw ResourceNotFoundException("Talent profile not available for resume")

        return objectMapper.convertValue(parsedData, TalentProfile::class.java)
    }

    @Transactional(readOnly = true)
    fun listEvaluationHistoryForJob(jobId: UUID, limit: Int = 6): List<JobEvaluationVersionResponse> {
        val normalizedLimit = limit.coerceIn(1, 20)
        val runs = jobEvaluationRunRepository.findByJobIdOrderByVersionNumberDesc(jobId, PageRequest.of(0, normalizedLimit))
        return runs.mapIndexed { index, run ->
            toEvaluationVersionResponse(run, runs.getOrNull(index + 1))
        }
    }

    @Transactional
    fun generateRecommendationsForJob(
        jobId: UUID,
        requestWeights: EvaluationRequestWeightsDTO? = null,
        evaluatedByUsername: String? = null,
        persistEvaluationRun: Boolean = false,
    ): JobEvaluationResponse {
        val job = jobRepository.findById(jobId)
            .orElseThrow { ResourceNotFoundException("Job not found") }
        val appliedWeights = try {
            AppliedEvaluationWeights.fromRequest(requestWeights)
        } catch (exception: IllegalArgumentException) {
            throw BadRequestException(
                message = exception.message ?: "Invalid evaluation weights",
                errorCode = ApiErrorCode.INVALID_EVALUATION_WEIGHTS,
                retryable = true,
                userHint = "请至少保留一个大于 0 的评估权重后重试。",
            )
        }
        val previousEvaluationRun = if (persistEvaluationRun) {
            jobEvaluationRunRepository.findTopByJobIdOrderByVersionNumberDesc(jobId)
        } else {
            null
        }
        val evaluationNote = requestWeights?.evaluationNote?.trim()?.takeIf { it.isNotEmpty() }
        val evaluator = if (persistEvaluationRun && evaluatedByUsername != null) {
            userRepository.findByUsername(evaluatedByUsername)
                .orElseThrow { ResourceNotFoundException("User not found") }
        } else {
            null
        }

        val jobEmbedding = ensureJobEmbedding(job)
        val parsedResumes = resumeRepository.findAll()
            .asSequence()
            .filter { it.status == "PARSED" && it.parsedData != null }
            .mapNotNull { resume ->
                val profile = parseTalentProfileOrNull(resume) ?: return@mapNotNull null
                ensureResumeEmbedding(resume)
                resume to profile
            }
            .toList()

        val semanticDistanceByResumeId = resolveSemanticDistances(jobEmbedding, parsedResumes.map { it.first })
        val skillNormalizer = buildSkillNormalizationMap()

        val scoredRecommendations = parsedResumes
            .asSequence()
            .map { (resume, profile) ->
                val cosineDistance = semanticDistanceByResumeId[requireNotNull(resume.id)] ?: BigDecimal.ONE
                scoreResume(job, resume, profile, cosineDistance, appliedWeights, skillNormalizer)
            }
            .sortedByDescending { it.matchScore }
            .toList()

        jobRecommendationRepository.deleteByJobId(jobId)
        jobRecommendationRepository.flush()

        val recommendationResponses = if (scoredRecommendations.isEmpty()) {
            emptyList()
        } else {
            val savedRecommendations = jobRecommendationRepository.saveAll(
                scoredRecommendations.map {
                    JobRecommendation(
                        job = job,
                        resume = it.resume,
                        matchScore = it.matchScore,
                        semanticScore = it.semanticScore,
                        xaiReasoning = it.xaiReasoning,
                        suitabilityReport = it.suitabilityReport,
                        matchedSkills = it.matchedSkills.sorted(),
                        missingSkills = it.missingSkills.sorted(),
                        xaiReport = structuredReportToMap(it.xaiReport),
                    )
                },
            ).sortedByDescending { it.matchScore }

            val profileByResumeId = scoredRecommendations.associateBy { requireNotNull(it.resume.id) }
            savedRecommendations.map { recommendation ->
                val scored = profileByResumeId.getValue(requireNotNull(recommendation.resume.id))
                toRecommendationResponse(recommendation, scored.profile, toScoreBreakdown(scored))
            }
        }

        val currentEvaluationRun = if (persistEvaluationRun) {
            jobEvaluationRunRepository.save(
                JobEvaluationRun(
                    job = job,
                    evaluatedBy = evaluator,
                    versionNumber = (previousEvaluationRun?.versionNumber ?: 0) + 1,
                    skillWeight = appliedWeights.skillWeight,
                    experienceWeight = appliedWeights.experienceWeight,
                    educationWeight = appliedWeights.educationWeight,
                    semanticWeight = appliedWeights.semanticWeight,
                    evaluatedCount = recommendationResponses.size,
                    evaluationNote = evaluationNote,
                    recommendationSnapshot = buildEvaluationSnapshot(recommendationResponses),
                ),
            )
        } else {
            null
        }

        return JobEvaluationResponse(
            jobId = requireNotNull(job.id),
            evaluatedCount = recommendationResponses.size,
            appliedWeights = appliedWeights,
            currentEvaluation = currentEvaluationRun?.let { toEvaluationVersionResponse(it, previousEvaluationRun) },
            previousEvaluation = previousEvaluationRun?.let { toEvaluationVersionResponse(it, null) },
            recommendations = recommendationResponses,
        )
    }

    @Transactional
    fun generateJobMatchesForCandidate(username: String): CandidateJobMatchResponse {
        val user = userRepository.findByUsername(username)
            .orElseThrow { ResourceNotFoundException("User not found") }
        if (user.role != UserRole.CANDIDATE) {
            throw BadRequestException(
                message = "Current user is not a candidate",
                errorCode = ApiErrorCode.CANDIDATE_ROLE_REQUIRED,
                retryable = false,
                userHint = "请使用候选人账号访问该功能。",
            )
        }

        val candidateId = requireNotNull(user.id)
        val parsedResume = resumeRepository.findTopByOwnerUserIdAndStatusOrderByUpdatedAtDesc(candidateId, ResumeService.STATUS_PARSED)
            ?: throw BadRequestException(
                message = "Candidate must upload and parse a resume before matching jobs",
                errorCode = ApiErrorCode.CANDIDATE_RESUME_REQUIRED,
                retryable = true,
                userHint = "请先在我的档案中上传并完成简历解析，再生成岗位匹配结果。",
            )
        val ignoredJobIds = candidateJobActionService.getIgnoredJobIds(candidateId)
        val jobs = jobRepository.findAll()
            .filterNot { ignoredJobIds.contains(it.id) }

        if (jobs.isEmpty()) {
            return CandidateJobMatchResponse(
                candidateUserId = candidateId,
                evaluatedCount = 0,
                recommendations = emptyList(),
            )
        }

        val resumeId = requireNotNull(parsedResume.id)
        var persistedRecommendations = jobRecommendationRepository.findByResumeId(resumeId)
        if (shouldRefreshCandidateRecommendations(parsedResume, jobs, ignoredJobIds, persistedRecommendations)) {
            refreshRecommendationsForResume(resumeId)
            persistedRecommendations = jobRecommendationRepository.findByResumeId(resumeId)
        }

        val actionStateByJobId = candidateJobActionService.getActionStateMap(candidateId, jobs.mapNotNull { it.id })
        val recommendations = persistedRecommendations.asSequence()
            .filterNot { ignoredJobIds.contains(it.job.id) }
            .sortedByDescending { it.matchScore }
            .map { recommendation ->
                toCandidateRecommendationResponse(
                    recommendation,
                    actionStateByJobId[requireNotNull(recommendation.job.id)] ?: JobActionStateResponse(false, false, false),
                )
            }
            .toList()

        return CandidateJobMatchResponse(
            candidateUserId = candidateId,
            evaluatedCount = recommendations.size,
            recommendations = recommendations,
        )
    }

    @Transactional(readOnly = true)
    fun listRecommendationsForJob(jobId: UUID): List<JobRecommendationResponse> {
        val job = jobRepository.findById(jobId)
            .orElseThrow { ResourceNotFoundException("Job not found") }
        val skillNormalizer = buildSkillNormalizationMap()

        return jobRecommendationRepository.findByJobId(jobId)
            .sortedByDescending { it.matchScore }
            .map { recommendation ->
                val profile = parseTalentProfileOrNull(recommendation.resume)
                toRecommendationResponse(
                    recommendation,
                    profile,
                    buildRecommendationScoreBreakdown(job, recommendation, profile, skillNormalizer),
                )
            }
    }

    @Transactional(readOnly = true)
    fun listRecommendationsForResume(resumeId: UUID) = jobRecommendationRepository.findByResumeId(resumeId)

    @Transactional
    fun refreshRecommendationsForResume(resumeId: UUID): List<JobRecommendation> {
        val resume = resumeRepository.findById(resumeId)
            .orElseThrow { ResourceNotFoundException("Resume not found") }

        if (resume.status != ResumeService.STATUS_PARSED || resume.parsedData == null) {
            deleteRecommendationsForResume(resumeId)
            return emptyList()
        }

        val profile = parseTalentProfileOrNull(resume)
            ?: throw BadRequestException("Candidate resume is missing a valid talent profile")
        val candidateEmbedding = ensureResumeEmbedding(resume)
        val weights = AppliedEvaluationWeights.fromRequest(null)
        val jobs = jobRepository.findAll().onEach { ensureJobEmbedding(it) }

        if (jobs.isEmpty()) {
            deleteRecommendationsForResume(resumeId)
            return emptyList()
        }

        val semanticDistanceByJobId = resolveJobSemanticDistances(candidateEmbedding, jobs)
        val skillNormalizer = buildSkillNormalizationMap()
        val scoredRecommendations = jobs.asSequence()
            .map { job ->
                val cosineDistance = semanticDistanceByJobId[requireNotNull(job.id)] ?: BigDecimal.ONE
                scoreResume(job, resume, profile, cosineDistance, weights, skillNormalizer)
            }
            .sortedByDescending { it.matchScore }
            .toList()

        jobRecommendationRepository.deleteByResumeId(resumeId)
        jobRecommendationRepository.flush()

        return jobRecommendationRepository.saveAll(
            scoredRecommendations.map {
                JobRecommendation(
                    job = it.job,
                    resume = it.resume,
                    matchScore = it.matchScore,
                    semanticScore = it.semanticScore,
                    xaiReasoning = it.xaiReasoning,
                    suitabilityReport = it.suitabilityReport,
                    matchedSkills = it.matchedSkills.sorted(),
                    missingSkills = it.missingSkills.sorted(),
                    xaiReport = structuredReportToMap(it.xaiReport),
                )
            },
        ).sortedByDescending { it.matchScore }
    }

    @Transactional
    fun deleteRecommendationsForResume(resumeId: UUID) {
        jobRecommendationRepository.deleteByResumeId(resumeId)
        jobRecommendationRepository.flush()
    }

    private fun scoreResume(
        job: Job,
        resume: Resume,
        profile: TalentProfile,
        cosineDistance: BigDecimal,
        weights: AppliedEvaluationWeights,
        skillNormalizer: Map<String, String>,
    ): ScoredRecommendation {
        val skillBreakdown = calculateSkillScore(job, profile, skillNormalizer)
        val experienceBreakdown = calculateExperienceScore(job, profile)
        val educationBreakdown = calculateEducationScore(job, profile)
        
        val candidateSkillTerms = normalizeToCanonicalTokens(profile.skills.map { it.name }, skillNormalizer)
        val requiredSkillLabels = extractRequiredSkillLabels(job)
        val matchedSkillLabels = requiredSkillLabels.filter { label ->
            normalizeToCanonicalTokens(listOf(label), skillNormalizer).all { token -> candidateSkillTerms.contains(token) }
        }.toSet()
        val missingSkillLabels = requiredSkillLabels.filterNot { label ->
            normalizeToCanonicalTokens(listOf(label), skillNormalizer).all { token -> candidateSkillTerms.contains(token) }
        }.toSet()
        val semanticScore = BigDecimal.valueOf((1.0 - cosineDistance.toDouble()).coerceIn(0.0, 1.0) * 100.0)
            .setScale(2, RoundingMode.HALF_UP)
        val hybridScore = BigDecimal.valueOf(
            skillBreakdown.score.toDouble() * weights.skillWeight.toDouble() / 100.0 +
                experienceBreakdown.score.toDouble() * weights.experienceWeight.toDouble() / 100.0 +
                educationBreakdown.score.toDouble() * weights.educationWeight.toDouble() / 100.0 +
                semanticScore.toDouble() * weights.semanticWeight.toDouble() / 100.0,
        )
            .setScale(2, RoundingMode.HALF_UP)

        val xaiReport = jobFitReportService.generate(
            JobFitReportRequest(
                audience = "shared",
                candidateName = profile.basicInfo.fullName,
                jobTitle = job.title,
                jobDescription = job.description,
                jobRequirements = job.requirements,
                matchScore = hybridScore,
                semanticScore = semanticScore,
                skillScore = skillBreakdown.score,
                experienceScore = experienceBreakdown.score,
                educationScore = educationBreakdown.score,
                matchedSkills = matchedSkillLabels.sorted(),
                missingSkills = missingSkillLabels.sorted(),
            ),
        )

        val normalizedMatchedSkills = matchedSkillLabels.ifEmpty { skillBreakdown.matchedSkills }
        val normalizedMissingSkills = missingSkillLabels.ifEmpty { skillBreakdown.missingSkills }
        val reasoning = buildSharedReasoning(
            job = job,
            matchScore = hybridScore,
            semanticScore = semanticScore,
            matchedSkills = normalizedMatchedSkills,
            missingSkills = normalizedMissingSkills,
            experienceBreakdown = experienceBreakdown,
            educationBreakdown = educationBreakdown,
        )
        val suitabilityReport = buildSuitabilityReport(
            xaiReport = xaiReport,
            missingSkills = normalizedMissingSkills,
            experienceBreakdown = experienceBreakdown,
            educationBreakdown = educationBreakdown,
        )

        return ScoredRecommendation(
            job = job,
            resume = resume,
            profile = profile,
            skillScore = skillBreakdown.score,
            experienceScore = experienceBreakdown.score,
            educationScore = educationBreakdown.score,
            semanticScore = semanticScore,
            cosineDistance = cosineDistance.setScale(4, RoundingMode.HALF_UP),
            matchScore = hybridScore,
            matchedSkills = normalizedMatchedSkills,
            missingSkills = normalizedMissingSkills,
            xaiReasoning = reasoning,
            suitabilityReport = suitabilityReport,
            xaiReport = xaiReport,
        )
    }

    private fun calculateSkillScore(job: Job, profile: TalentProfile, skillNormalizer: Map<String, String>): SkillScoreBreakdown {
        val requiredSkills = extractRequiredSkillTerms(job, skillNormalizer)
        val jobKeywords = normalizeToCanonicalTokens(extractJobKeywords(job), skillNormalizer)
        val candidateSkillTerms = normalizeToCanonicalTokens(profile.skills.map { it.name }, skillNormalizer)

        val matchedSkills = requiredSkills.intersect(candidateSkillTerms)
        val missingSkills = requiredSkills - candidateSkillTerms
        val keywordHits = (jobKeywords - requiredSkills).intersect(candidateSkillTerms)

        val score = if (requiredSkills.isEmpty()) {
            BigDecimal("60.00")
        } else {
            BigDecimal.valueOf(matchedSkills.size.toDouble() / requiredSkills.size.toDouble() * 100.0)
                .setScale(2, RoundingMode.HALF_UP)
        }

        return SkillScoreBreakdown(
            matchedSkills = matchedSkills,
            missingSkills = missingSkills,
            keywordHits = keywordHits,
            score = score,
        )
    }

    private fun calculateExperienceScore(job: Job, profile: TalentProfile): ExperienceScoreBreakdown {
        val requiredYears = parseExperienceYears(job.requirements?.get("experienceYears"))
        val candidateYears = estimateExperienceYears(profile)
        val experienceKeywords = extractExperienceKeywords(job)
        val candidateExperienceCorpus = buildSet {
            addAll(tokenize(profile.basicInfo.summary))
            addAll(tokenize(profile.basicInfo.headline))
            profile.workExperiences.forEach { experience ->
                addAll(tokenize(experience.company))
                addAll(tokenize(experience.title))
                experience.responsibilities.forEach { addAll(tokenize(it)) }
                experience.achievements.forEach { addAll(tokenize(it)) }
            }
        }
        val matchedKeywords = experienceKeywords.intersect(candidateExperienceCorpus)

        val yearsScore = when {
            requiredYears == null && profile.workExperiences.isNotEmpty() -> 70.0
            requiredYears == null -> 0.0
            requiredYears.compareTo(BigDecimal.ZERO) == 0 -> 70.0
            else -> candidateYears.divide(requiredYears, 4, RoundingMode.HALF_UP)
                .coerceAtMost(BigDecimal.ONE)
                .toDouble() * 70.0
        }
        val keywordScore = if (experienceKeywords.isEmpty()) {
            if (profile.workExperiences.isNotEmpty()) 30.0 else 0.0
        } else {
            matchedKeywords.size.toDouble() / experienceKeywords.size.toDouble() * 30.0
        }

        return ExperienceScoreBreakdown(
            candidateYears = candidateYears.setScale(2, RoundingMode.HALF_UP),
            requiredYears = requiredYears?.setScale(2, RoundingMode.HALF_UP),
            matchedKeywords = matchedKeywords,
            score = BigDecimal.valueOf(yearsScore + keywordScore).setScale(2, RoundingMode.HALF_UP),
        )
    }

    private fun calculateEducationScore(job: Job, profile: TalentProfile): EducationScoreBreakdown {
        val educationKeywords = extractEducationKeywords(job)
        val candidateEducationCorpus = buildSet {
            profile.educationExperiences.forEach { education ->
                addAll(tokenize(education.school))
                addAll(tokenize(education.degree))
                addAll(tokenize(education.fieldOfStudy))
            }
        }
        val matchedKeywords = educationKeywords.intersect(candidateEducationCorpus)
        val score = when {
            profile.educationExperiences.isEmpty() -> BigDecimal.ZERO
            educationKeywords.isEmpty() -> BigDecimal("70.00")
            else -> BigDecimal.valueOf(
                40.0 + matchedKeywords.size.toDouble() / educationKeywords.size.toDouble() * 60.0,
            ).setScale(2, RoundingMode.HALF_UP)
        }

        return EducationScoreBreakdown(
            matchedKeywords = matchedKeywords,
            hasEducation = profile.educationExperiences.isNotEmpty(),
            score = score,
        )
    }

    private fun toRecommendationResponse(
        recommendation: JobRecommendation,
        profile: TalentProfile?,
        scoreBreakdown: JobRecommendationScoreBreakdown,
    ): JobRecommendationResponse {
        val resume = recommendation.resume
        val basicInfo = profile?.basicInfo

        return JobRecommendationResponse(
            id = requireNotNull(recommendation.id),
            jobId = requireNotNull(recommendation.job.id),
            resumeId = requireNotNull(resume.id),
            matchScore = recommendation.matchScore,
            scoreBreakdown = scoreBreakdown,
            xaiReasoning = recommendation.xaiReasoning,
            xaiReport = deserializeStructuredReport(recommendation.xaiReport),
            candidate = JobRecommendationCandidateResponse(
                candidateName = basicInfo?.fullName ?: resume.candidateName,
                contactInfo = resume.contactInfo,
                status = resume.status,
                basicInfo = basicInfo,
                radarScores = profile?.radarScores,
                skills = profile?.skills ?: emptyList(),
                parsedData = resume.parsedData,
            ),
            createdAt = recommendation.createdAt,
            updatedAt = recommendation.updatedAt,
        )
    }

    private fun buildEvaluationSnapshot(recommendations: List<JobRecommendationResponse>): List<Map<String, Any>> {
        return recommendations.mapIndexed { index, recommendation ->
            buildMap<String, Any> {
                put("rank", index + 1)
                put("resumeId", recommendation.resumeId.toString())
                put("matchScore", recommendation.matchScore)
                (recommendation.candidate.basicInfo?.fullName ?: recommendation.candidate.candidateName)?.let {
                    put("candidateName", it)
                }
            }
        }
    }

    private fun toEvaluationVersionResponse(
        run: JobEvaluationRun,
        previousRun: JobEvaluationRun? = null,
    ): JobEvaluationVersionResponse {
        return JobEvaluationVersionResponse(
            evaluationId = requireNotNull(run.id),
            versionNumber = run.versionNumber,
            evaluatedAt = run.createdAt,
            evaluatedByUsername = run.evaluatedBy?.username,
            evaluatedCount = run.evaluatedCount,
            evaluationNote = run.evaluationNote,
            appliedWeights = AppliedEvaluationWeights(
                skillWeight = run.skillWeight,
                experienceWeight = run.experienceWeight,
                educationWeight = run.educationWeight,
                semanticWeight = run.semanticWeight,
            ),
            comparisonToPrevious = previousRun?.let { buildEvaluationDeltaSummary(run, it) },
            topRecommendations = extractEvaluationRecommendations(run),
        )
    }

    private fun extractEvaluationRecommendations(run: JobEvaluationRun): List<JobEvaluationRecommendationSnapshot> {
        return run.recommendationSnapshot.mapNotNull(::toEvaluationRecommendationSnapshot).take(3)
    }

    private fun buildEvaluationDeltaSummary(
        run: JobEvaluationRun,
        previousRun: JobEvaluationRun,
    ): JobEvaluationDeltaSummary? {
        val weightChanges = listOf(
            buildWeightDelta("skillWeight", "技能", previousRun.skillWeight, run.skillWeight),
            buildWeightDelta("experienceWeight", "经验", previousRun.experienceWeight, run.experienceWeight),
            buildWeightDelta("educationWeight", "教育", previousRun.educationWeight, run.educationWeight),
            buildWeightDelta("semanticWeight", "语义", previousRun.semanticWeight, run.semanticWeight),
        )
            .filterNotNull()
            .sortedWith(compareByDescending<JobEvaluationWeightDelta> { it.deltaWeight.abs() }.thenBy { it.label })

        val topCandidateChange = buildTopCandidateChange(run, previousRun)
        if (weightChanges.isEmpty() && topCandidateChange == null) {
            return null
        }

        val summaryParts = mutableListOf<String>()
        weightChanges.firstOrNull()?.let { dominant ->
            summaryParts += "主要调权变化为${dominant.label}${if (dominant.deltaWeight > BigDecimal.ZERO) "上调" else "下调"} ${formatDecimal(dominant.deltaWeight.abs())}%"
        }
        topCandidateChange?.let { topChange ->
            when {
                topChange.changed -> summaryParts += "Top1 由 ${topChange.previousCandidateName ?: "未命名候选人"} 切换为 ${topChange.currentCandidateName ?: "未命名候选人"}"
                topChange.scoreDelta != null && topChange.scoreDelta.compareTo(BigDecimal.ZERO) != 0 -> summaryParts += "Top1 ${topChange.currentCandidateName ?: "未命名候选人"} 分数${if (topChange.scoreDelta > BigDecimal.ZERO) "上升" else "下降"} ${formatDecimal(topChange.scoreDelta.abs())} 分"
                topChange.enteredCandidates.isNotEmpty() || topChange.droppedCandidates.isNotEmpty() -> summaryParts += "Top3 名单已发生调整"
            }
        }

        return JobEvaluationDeltaSummary(
            summary = summaryParts.ifEmpty { listOf("本版与上一版暂无显著变化。") }.joinToString("；"),
            weightChanges = weightChanges,
            topCandidateChange = topCandidateChange,
        )
    }

    private fun buildWeightDelta(
        dimension: String,
        label: String,
        previousWeight: BigDecimal,
        currentWeight: BigDecimal,
    ): JobEvaluationWeightDelta? {
        val delta = currentWeight.subtract(previousWeight).setScale(2, RoundingMode.HALF_UP)
        if (delta.compareTo(BigDecimal.ZERO) == 0) {
            return null
        }
        return JobEvaluationWeightDelta(
            dimension = dimension,
            label = label,
            previousWeight = previousWeight,
            currentWeight = currentWeight,
            deltaWeight = delta,
        )
    }

    private fun buildTopCandidateChange(
        run: JobEvaluationRun,
        previousRun: JobEvaluationRun,
    ): JobEvaluationTopCandidateChange? {
        val currentTopRecommendations = extractEvaluationRecommendations(run)
        val previousTopRecommendations = extractEvaluationRecommendations(previousRun)
        val currentTop = currentTopRecommendations.firstOrNull()
        val previousTop = previousTopRecommendations.firstOrNull()

        val enteredCandidates = currentTopRecommendations
            .filterNot { current -> previousTopRecommendations.any { it.resumeId == current.resumeId } }
            .map { it.candidateName ?: "未命名候选人" }
        val droppedCandidates = previousTopRecommendations
            .filterNot { previous -> currentTopRecommendations.any { it.resumeId == previous.resumeId } }
            .map { it.candidateName ?: "未命名候选人" }

        if (currentTop == null && previousTop == null && enteredCandidates.isEmpty() && droppedCandidates.isEmpty()) {
            return null
        }

        val scoreDelta = if (currentTop != null && previousTop != null && currentTop.resumeId == previousTop.resumeId) {
            currentTop.matchScore.subtract(previousTop.matchScore).setScale(2, RoundingMode.HALF_UP)
        } else {
            null
        }

        return JobEvaluationTopCandidateChange(
            changed = currentTop?.resumeId != previousTop?.resumeId,
            previousCandidateName = previousTop?.candidateName,
            currentCandidateName = currentTop?.candidateName,
            previousMatchScore = previousTop?.matchScore,
            currentMatchScore = currentTop?.matchScore,
            scoreDelta = scoreDelta,
            enteredCandidates = enteredCandidates,
            droppedCandidates = droppedCandidates,
        )
    }

    private fun formatDecimal(value: BigDecimal): String {
        return value.stripTrailingZeros().toPlainString()
    }

    private fun toEvaluationRecommendationSnapshot(snapshot: Map<String, Any>): JobEvaluationRecommendationSnapshot? {
        val rank = (snapshot["rank"] as? Number)?.toInt() ?: return null
        val resumeId = (snapshot["resumeId"] as? String)?.let(UUID::fromString) ?: return null
        val candidateName = snapshot["candidateName"] as? String
        val matchScore = toBigDecimal(snapshot["matchScore"]) ?: return null
        return JobEvaluationRecommendationSnapshot(
            rank = rank,
            resumeId = resumeId,
            candidateName = candidateName,
            matchScore = matchScore,
        )
    }

    private fun toBigDecimal(value: Any?): BigDecimal? {
        return when (value) {
            is BigDecimal -> value
            is Int -> BigDecimal(value)
            is Long -> BigDecimal(value)
            is Double -> BigDecimal.valueOf(value)
            is Float -> BigDecimal.valueOf(value.toDouble())
            is String -> value.toBigDecimalOrNull()
            else -> null
        }
    }

    private fun buildRecommendationScoreBreakdown(
        job: Job,
        recommendation: JobRecommendation,
        profile: TalentProfile?,
        skillNormalizer: Map<String, String>,
    ): JobRecommendationScoreBreakdown {
        if (profile == null) {
            return JobRecommendationScoreBreakdown(
                skillScore = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                experienceScore = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                educationScore = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                semanticScore = recommendation.semanticScore,
            )
        }

        val skillBreakdown = calculateSkillScore(job, profile, skillNormalizer)
        val experienceBreakdown = calculateExperienceScore(job, profile)
        val educationBreakdown = calculateEducationScore(job, profile)
        return JobRecommendationScoreBreakdown(
            skillScore = skillBreakdown.score,
            experienceScore = experienceBreakdown.score,
            educationScore = educationBreakdown.score,
            semanticScore = recommendation.semanticScore,
        )
    }

    private fun toScoreBreakdown(scoredRecommendation: ScoredRecommendation): JobRecommendationScoreBreakdown {
        return JobRecommendationScoreBreakdown(
            skillScore = scoredRecommendation.skillScore,
            experienceScore = scoredRecommendation.experienceScore,
            educationScore = scoredRecommendation.educationScore,
            semanticScore = scoredRecommendation.semanticScore,
        )
    }

    private fun toCandidateRecommendationResponse(
        recommendation: JobRecommendation,
        actionState: JobActionStateResponse,
    ): CandidateJobRecommendationResponse {
        val xaiReport = deserializeStructuredReport(recommendation.xaiReport)
            ?: fallbackStructuredReport(recommendation)

        return CandidateJobRecommendationResponse(
            jobId = requireNotNull(recommendation.job.id),
            title = recommendation.job.title,
            description = recommendation.job.description,
            organization = OrganizationRefResponse.from(recommendation.job.organization),
            requirements = recommendation.job.requirements,
            matchScore = recommendation.matchScore,
            semanticScore = recommendation.semanticScore,
            suitabilityReport = recommendation.suitabilityReport.ifBlank { xaiReport.narrative },
            xaiReport = xaiReport,
            matchedSkills = recommendation.matchedSkills.sorted(),
            missingSkills = recommendation.missingSkills.sorted(),
            actionState = actionState,
            createdAt = recommendation.job.createdAt,
        )
    }

    private fun shouldRefreshCandidateRecommendations(
        resume: Resume,
        jobs: List<Job>,
        ignoredJobIds: Set<UUID>,
        persistedRecommendations: List<JobRecommendation>,
    ): Boolean {
        if (persistedRecommendations.isEmpty()) {
            return true
        }

        val expectedJobIds = jobs.mapNotNull { it.id }.toSet()
        val persistedJobIds = persistedRecommendations
            .filterNot { ignoredJobIds.contains(requireNotNull(it.job.id)) }
            .mapNotNull { it.job.id }
            .toSet()
        if (expectedJobIds != persistedJobIds) {
            return true
        }

        return persistedRecommendations.any { recommendation ->
            recommendation.updatedAt.isBefore(resume.updatedAt) || recommendation.updatedAt.isBefore(recommendation.job.updatedAt)
        }
    }

    private fun buildSharedReasoning(
        job: Job,
        matchScore: BigDecimal,
        semanticScore: BigDecimal,
        matchedSkills: Set<String>,
        missingSkills: Set<String>,
        experienceBreakdown: ExperienceScoreBreakdown,
        educationBreakdown: EducationScoreBreakdown,
    ): String {
        return buildString {
            append("围绕岗位“")
            append(job.title)
            append("”完成综合评估：当前匹配度为 ")
            append(matchScore)
            append("%，语义相似度为 ")
            append(semanticScore)
            append("%。已匹配技能：")
            append(matchedSkills.sorted().joinToString("、").ifBlank { "暂无显式命中" })
            append("；待补强技能：")
            append(missingSkills.sorted().joinToString("、").ifBlank { "暂无明显缺口" })
            append("；经验评估：")
            append(experienceBreakdown.candidateYears)
            append(experienceBreakdown.requiredYears?.let { " 年，对标岗位要求 $it 年" } ?: " 年，岗位未设置明确年限")
            append("；教育证据：")
            append(
                educationBreakdown.matchedKeywords.sorted().joinToString("、").ifBlank {
                    if (educationBreakdown.hasEducation) "已检测到基础教育经历" else "暂未检测到有效教育信息"
                },
            )
            append("。")
        }
    }

    private fun buildSuitabilityReport(
        xaiReport: StructuredJobFitReport,
        missingSkills: Set<String>,
        experienceBreakdown: ExperienceScoreBreakdown,
        educationBreakdown: EducationScoreBreakdown,
    ): String {
        val narrative = xaiReport.narrative.trim()
        val normalizedSummary = when {
            narrative.isNotBlank() && CJK_TEXT_REGEX.containsMatchIn(narrative) -> narrative.removePrefix("岗位适应性报告与技能提升建议：").trim()
            xaiReport.fitBand.equals("HIGH", ignoreCase = true) -> "当前整体匹配度较高，已经具备较强的岗位适配基础。"
            xaiReport.fitBand.equals("MEDIUM", ignoreCase = true) -> "当前具备一定岗位基础，但仍存在需要优先补齐的关键差距。"
            else -> "当前与岗位要求仍有明显差距，需要围绕核心技能和经历进行定向补强。"
        }
        val improvementAdvice = when {
            missingSkills.isNotEmpty() -> "建议优先补强 ${missingSkills.sorted().take(3).joinToString("、")}，这样能最快提升岗位竞争力。"
            experienceBreakdown.requiredYears != null && experienceBreakdown.candidateYears < experienceBreakdown.requiredYears -> "建议继续积累与岗位职责更贴近的项目经验，并把成果量化写进简历。"
            !educationBreakdown.hasEducation -> "建议补充教育背景、训练营或认证信息，方便招聘方判断基础能力。"
            else -> "当前核心能力已经较贴合岗位，下一步更适合强化项目成果、业务指标和技术深度证明。"
        }

        return buildString {
            append("岗位适应性报告与技能提升建议：")
            append(normalizedSummary)
            append(" ")
            append(improvementAdvice)
        }
    }

    private fun deserializeStructuredReport(report: Map<String, Any>?): StructuredJobFitReport? {
        return report?.let { objectMapper.convertValue(it, StructuredJobFitReport::class.java) }
    }

    private fun fallbackStructuredReport(recommendation: JobRecommendation): StructuredJobFitReport {
        val fitBand = when {
            recommendation.matchScore >= BigDecimal("80") -> "HIGH"
            recommendation.matchScore >= BigDecimal("55") -> "MEDIUM"
            else -> "LOW"
        }
        return StructuredJobFitReport(
            headline = "共享匹配报告待补全",
            fitBand = fitBand,
            summary = recommendation.xaiReasoning,
            strengths = recommendation.matchedSkills.take(3).map { "已匹配技能：$it" },
            risks = recommendation.missingSkills.take(3).map { "待补强技能：$it" },
            improvementSuggestions = recommendation.missingSkills.take(3).map { "优先补强 $it" },
            nextSteps = listOf("查看完整岗位要求", "结合最近经历补齐关键证据"),
            narrative = recommendation.suitabilityReport.ifBlank { recommendation.xaiReasoning },
        )
    }

    private fun structuredReportToMap(report: StructuredJobFitReport): Map<String, Any> {
        return objectMapper.convertValue(report, object : TypeReference<Map<String, Any>>() {})
    }

    private fun parseTalentProfileOrNull(resume: Resume): TalentProfile? {
        val parsedData = resume.parsedData ?: return null
        return runCatching { objectMapper.convertValue(parsedData, TalentProfile::class.java) }
            .getOrNull()
    }

    private fun ensureJobEmbedding(job: Job): String {
        currentJobEmbedding(job)?.let { return it }

        val embedding = embeddingService.generateJobEmbedding(job)
        val jobId = requireNotNull(job.id)
        if (embeddingService.shouldUseNativeVectorStorage()) {
            jobRepository.updateEmbedding(jobId, embedding)
            job.embedding = embedding
        } else {
            job.runtimeEmbedding = embedding
        }
        job.runtimeEmbedding = embedding
        return embedding
    }

    private fun ensureResumeEmbedding(resume: Resume): String {
        currentResumeEmbedding(resume)?.let { return it }

        val parsedData = resume.parsedData ?: return ""
        val embedding = embeddingService.generateResumeEmbedding(parsedData)
        val resumeId = requireNotNull(resume.id)
        if (embeddingService.shouldUseNativeVectorStorage()) {
            resumeRepository.updateEmbedding(resumeId, embedding)
            resume.embedding = embedding
        } else {
            resume.runtimeEmbedding = embedding
        }
        resume.runtimeEmbedding = embedding
        return embedding
    }

    private fun currentJobEmbedding(job: Job): String? {
        return job.runtimeEmbedding?.takeIf { it.isNotBlank() }
            ?: job.embedding?.takeIf { it.isNotBlank() }
    }

    private fun currentResumeEmbedding(resume: Resume): String? {
        return resume.runtimeEmbedding?.takeIf { it.isNotBlank() }
            ?: resume.embedding?.takeIf { it.isNotBlank() }
    }

    private fun resolveSemanticDistances(
        jobEmbedding: String,
        resumes: List<Resume>,
    ): Map<UUID, BigDecimal> {
        val resumeIds = resumes.mapNotNull { it.id }.toSet()
        if (resumeIds.isEmpty()) {
            return emptyMap()
        }

        if (embeddingService.shouldUsePgvectorQuery()) {
            return runCatching {
                resumeRepository.findSemanticMatches(jobEmbedding, limit = resumeIds.size.coerceAtLeast(100))
                    .filter { resumeIds.contains(it.getResumeId()) }
                    .associate { projection ->
                        projection.getResumeId() to BigDecimal.valueOf(projection.getCosineDistance())
                    }
            }.getOrElse {
                buildInMemorySemanticDistances(jobEmbedding, resumes)
            }
        }

        return buildInMemorySemanticDistances(jobEmbedding, resumes)
    }

    private fun buildInMemorySemanticDistances(
        jobEmbedding: String,
        resumes: List<Resume>,
    ): Map<UUID, BigDecimal> {
        return resumes.mapNotNull { resume ->
            val resumeId = resume.id ?: return@mapNotNull null
            val resumeEmbedding = currentResumeEmbedding(resume) ?: return@mapNotNull resumeId to BigDecimal.ONE
            val similarity = embeddingService.cosineSimilarity(jobEmbedding, resumeEmbedding)
            resumeId to BigDecimal.valueOf(1.0 - similarity).setScale(6, RoundingMode.HALF_UP)
        }.toMap()
    }

    private fun resolveJobSemanticDistances(
        candidateEmbedding: String,
        jobs: List<Job>,
    ): Map<UUID, BigDecimal> {
        val jobIds = jobs.mapNotNull { it.id }.toSet()
        if (jobIds.isEmpty()) {
            return emptyMap()
        }

        if (embeddingService.shouldUsePgvectorQuery()) {
            return runCatching {
                jobRepository.findSemanticMatches(candidateEmbedding, limit = jobIds.size.coerceAtLeast(100))
                    .filter { jobIds.contains(it.getJobId()) }
                    .associate { projection ->
                        projection.getJobId() to BigDecimal.valueOf(projection.getCosineDistance())
                    }
            }.getOrElse {
                buildInMemoryJobSemanticDistances(candidateEmbedding, jobs)
            }
        }

        return buildInMemoryJobSemanticDistances(candidateEmbedding, jobs)
    }

    private fun buildInMemoryJobSemanticDistances(
        candidateEmbedding: String,
        jobs: List<Job>,
    ): Map<UUID, BigDecimal> {
        return jobs.mapNotNull { job ->
            val jobId = job.id ?: return@mapNotNull null
            val jobEmbedding = currentJobEmbedding(job) ?: return@mapNotNull jobId to BigDecimal.ONE
            val similarity = embeddingService.cosineSimilarity(candidateEmbedding, jobEmbedding)
            jobId to BigDecimal.valueOf(1.0 - similarity).setScale(6, RoundingMode.HALF_UP)
        }.toMap()
    }

    private fun extractRequiredSkillLabels(job: Job): List<String> {
        return when (val value = job.requirements?.get("skills")) {
            is Collection<*> -> value.filterIsInstance<String>().map { it.trim() }.filter { it.isNotBlank() }
            is String -> listOf(value.trim()).filter { it.isNotBlank() }
            else -> emptyList()
        }
    }

    private fun parseExperienceYears(value: Any?): BigDecimal? {
        return when (value) {
            is Int -> BigDecimal(value)
            is Long -> BigDecimal(value)
            is Double -> BigDecimal.valueOf(value)
            is Float -> BigDecimal.valueOf(value.toDouble())
            is String -> value.toBigDecimalOrNull()
            else -> null
        }
    }

    private fun estimateExperienceYears(profile: TalentProfile): BigDecimal {
        if (profile.workExperiences.isEmpty()) {
            return BigDecimal.ZERO
        }

        val estimatedYears = profile.workExperiences.sumOf { experience ->
            estimateYearsBetween(experience.startDate, experience.endDate)
        }
        return BigDecimal.valueOf(estimatedYears)
    }

    private fun estimateYearsBetween(startDate: String?, endDate: String?): Double {
        val startYear = startDate?.take(4)?.toIntOrNull() ?: return 1.0
        val endYear = when {
            endDate.isNullOrBlank() -> java.time.Year.now().value
            endDate.startsWith("Present", ignoreCase = true) -> java.time.Year.now().value
            else -> endDate.take(4).toIntOrNull() ?: java.time.Year.now().value
        }
        return (endYear - startYear).coerceAtLeast(1).toDouble()
    }

    private fun extractExperienceKeywords(job: Job): Set<String> {
        val requirementKeywords = when (val value = job.requirements?.get("experienceKeywords")) {
            is Collection<*> -> value.filterIsInstance<String>().flatMap { tokenize(it) }
            is String -> tokenize(value)
            else -> emptyList()
        }

        val corpus = tokenize(job.title) + tokenize(job.description) + requirementKeywords
        return corpus.filterNot { it in setOf("junior", "senior", "mid") }.toSet()
    }

    private fun extractEducationKeywords(job: Job): Set<String> {
        val explicitKeywords = when (val value = job.requirements?.get("educationKeywords")) {
            is Collection<*> -> value.filterIsInstance<String>().flatMap { tokenize(it) }
            is String -> tokenize(value)
            else -> emptyList()
        }

        val degreeKeywords = listOf("bachelor", "master", "phd", "computer", "science", "engineering")
        val jobTokens = tokenize(job.title) + tokenize(job.description)
        return (explicitKeywords + jobTokens.filter { degreeKeywords.contains(it) }).toSet()
    }

    private fun extractRequiredSkillTerms(job: Job, skillNormalizer: Map<String, String>): Set<String> {
        val skillsFromRequirements = when (val value = job.requirements?.get("skills")) {
            is Collection<*> -> value.filterIsInstance<String>()
            is String -> listOf(value)
            else -> emptyList()
        }

        if (skillsFromRequirements.isNotEmpty()) {
            return normalizeToCanonicalTokens(skillsFromRequirements, skillNormalizer)
        }

        return normalizeToCanonicalTokens(listOf(job.title, job.description), skillNormalizer)
    }

    private fun extractJobKeywords(job: Job): Set<String> {
        val requirementKeywords = job.requirements
            ?.values
            ?.flatMap { value ->
                when (value) {
                    is String -> tokenize(value)
                    is Collection<*> -> value.filterIsInstance<String>().flatMap { tokenize(it) }
                    else -> emptyList()
                }
            }
            .orEmpty()

        return (tokenize(job.title) + tokenize(job.description) + requirementKeywords).toSet()
    }

    private fun tokenize(text: String?): List<String> {
        if (text.isNullOrBlank()) {
            return emptyList()
        }

        return text.lowercase()
            .split(TOKEN_SPLIT_REGEX)
            .asSequence()
            .map { it.trim() }
            .filter { it.length >= 2 }
            .filterNot { STOP_WORDS.contains(it) }
            .toList()
    }

    private fun buildSkillNormalizationMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        for (name in skillDictionaryRepository.findEnabledNamesOrderByNameAsc()) {
            val canonicalTokens = tokenize(name).joinToString(" ")
            if (canonicalTokens.isNotBlank()) {
                map[canonicalTokens] = canonicalTokens
            }
        }
        return map
    }

    private fun normalizeToCanonicalTokens(rawSkills: Collection<String?>, normalizer: Map<String, String>): Set<String> {
        return rawSkills.filterNotNull().flatMap { raw ->
            val rawTokens = tokenize(raw).joinToString(" ")
            val mapped = normalizer[rawTokens] ?: rawTokens
            mapped.split(" ")
        }.toSet()
    }
}