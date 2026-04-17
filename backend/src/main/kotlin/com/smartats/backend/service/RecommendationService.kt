package com.smartats.backend.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.smartats.backend.domain.Job
import com.smartats.backend.domain.JobRecommendation
import com.smartats.backend.domain.Resume
import com.smartats.backend.domain.UserRole
import com.smartats.backend.dto.candidate.CandidateJobMatchResponse
import com.smartats.backend.dto.candidate.CandidateJobRecommendationResponse
import com.smartats.backend.dto.candidate.JobActionStateResponse
import com.smartats.backend.dto.job.AppliedEvaluationWeights
import com.smartats.backend.dto.job.EvaluationRequestWeightsDTO
import com.smartats.backend.dto.job.JobEvaluationResponse
import com.smartats.backend.dto.job.JobRecommendationCandidateResponse
import com.smartats.backend.dto.job.JobRecommendationResponse
import com.smartats.backend.dto.talent.TalentProfile
import com.smartats.backend.dto.xai.JobFitReportRequest
import com.smartats.backend.dto.xai.StructuredJobFitReport
import com.smartats.backend.exception.BadRequestException
import com.smartats.backend.exception.ResourceNotFoundException
import com.smartats.backend.repository.JobRecommendationRepository
import com.smartats.backend.repository.JobRepository
import com.smartats.backend.repository.ResumeRepository
import com.smartats.backend.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

@Service
class RecommendationService(
    private val jobRepository: JobRepository,
    private val resumeRepository: ResumeRepository,
    private val jobRecommendationRepository: JobRecommendationRepository,
    private val userRepository: UserRepository,
    private val embeddingService: EmbeddingService,
    private val jobFitReportService: JobFitReportService,
    private val candidateJobActionService: CandidateJobActionService,
    private val objectMapper: ObjectMapper,
) {

    companion object {
        private val TOKEN_SPLIT_REGEX = Regex("[^a-z0-9+#.]+")
        private val STOP_WORDS = setOf(
            "a", "an", "and", "are", "as", "at", "be", "build", "by", "for", "from", "in", "into",
            "is", "of", "on", "or", "that", "the", "to", "with", "we", "you", "our", "will", "can",
        )
    }

    private data class ScoredRecommendation(
        val resume: Resume,
        val profile: TalentProfile,
        val skillScore: BigDecimal,
        val experienceScore: BigDecimal,
        val educationScore: BigDecimal,
        val semanticScore: BigDecimal,
        val cosineDistance: BigDecimal,
        val matchScore: BigDecimal,
        val xaiReasoning: String,
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

    private data class ReverseScoredJobRecommendation(
        val job: Job,
        val skillScore: BigDecimal,
        val experienceScore: BigDecimal,
        val educationScore: BigDecimal,
        val semanticScore: BigDecimal,
        val matchScore: BigDecimal,
        val matchedSkills: Set<String>,
        val missingSkills: Set<String>,
        val suitabilityReport: String,
        val xaiReport: StructuredJobFitReport,
    )

    @Transactional(readOnly = true)
    fun parseTalentProfile(resumeId: UUID): TalentProfile {
        val resume = resumeRepository.findById(resumeId)
            .orElseThrow { ResourceNotFoundException("Resume not found") }

        val parsedData = resume.parsedData
            ?: throw ResourceNotFoundException("Talent profile not available for resume")

        return objectMapper.convertValue(parsedData, TalentProfile::class.java)
    }

    @Transactional
    fun generateRecommendationsForJob(
        jobId: UUID,
        requestWeights: EvaluationRequestWeightsDTO? = null,
    ): JobEvaluationResponse {
        val job = jobRepository.findById(jobId)
            .orElseThrow { ResourceNotFoundException("Job not found") }
        val appliedWeights = try {
            AppliedEvaluationWeights.fromRequest(requestWeights)
        } catch (exception: IllegalArgumentException) {
            throw BadRequestException(exception.message ?: "Invalid evaluation weights")
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

        val scoredRecommendations = parsedResumes
            .asSequence()
            .map { (resume, profile) ->
                val cosineDistance = semanticDistanceByResumeId[requireNotNull(resume.id)] ?: BigDecimal.ONE
                scoreResume(job, resume, profile, cosineDistance, appliedWeights)
            }
            .sortedByDescending { it.matchScore }
            .toList()

        jobRecommendationRepository.deleteByJobId(jobId)
        jobRecommendationRepository.flush()

        if (scoredRecommendations.isEmpty()) {
            return JobEvaluationResponse(
                jobId = requireNotNull(job.id),
                evaluatedCount = 0,
                appliedWeights = appliedWeights,
                recommendations = emptyList(),
            )
        }

        val savedRecommendations = jobRecommendationRepository.saveAll(
            scoredRecommendations.map {
                JobRecommendation(
                    job = job,
                    resume = it.resume,
                    matchScore = it.matchScore,
                    xaiReasoning = it.xaiReasoning,
                    xaiReport = objectMapper.convertValue(it.xaiReport, Map::class.java) as Map<String, Any>,
                )
            },
        ).sortedByDescending { it.matchScore }

        val profileByResumeId = scoredRecommendations.associateBy { requireNotNull(it.resume.id) }

        return JobEvaluationResponse(
            jobId = requireNotNull(job.id),
            evaluatedCount = savedRecommendations.size,
            appliedWeights = appliedWeights,
            recommendations = savedRecommendations.map { recommendation ->
                val scored = profileByResumeId.getValue(requireNotNull(recommendation.resume.id))
                toRecommendationResponse(recommendation, scored.profile)
            },
        )
    }

    @Transactional
    fun generateJobMatchesForCandidate(username: String): CandidateJobMatchResponse {
        val user = userRepository.findByUsername(username)
            .orElseThrow { ResourceNotFoundException("User not found") }
        if (user.role != UserRole.CANDIDATE) {
            throw BadRequestException("Current user is not a candidate")
        }

        val candidateId = requireNotNull(user.id)
        val parsedResume = resumeRepository.findTopByOwnerUserIdAndStatusOrderByUpdatedAtDesc(candidateId, ResumeService.STATUS_PARSED)
            ?: throw BadRequestException("Candidate must upload and parse a resume before matching jobs")
        val profile = parseTalentProfileOrNull(parsedResume)
            ?: throw BadRequestException("Candidate resume is missing a valid talent profile")
        val candidateEmbedding = ensureResumeEmbedding(parsedResume)
        val weights = AppliedEvaluationWeights.fromRequest(null)
        val ignoredJobIds = candidateJobActionService.getIgnoredJobIds(candidateId)
        val jobs = jobRepository.findAll()
            .filterNot { ignoredJobIds.contains(it.id) }
            .onEach { ensureJobEmbedding(it) }

        if (jobs.isEmpty()) {
            return CandidateJobMatchResponse(
                candidateUserId = candidateId,
                evaluatedCount = 0,
                recommendations = emptyList(),
            )
        }

        val semanticDistanceByJobId = resolveJobSemanticDistances(candidateEmbedding, jobs)
        val actionStateByJobId = candidateJobActionService.getActionStateMap(candidateId, jobs.mapNotNull { it.id })
        val recommendations = jobs.asSequence()
            .map { job ->
                val cosineDistance = semanticDistanceByJobId[requireNotNull(job.id)] ?: BigDecimal.ONE
                scoreJobForCandidate(job, profile, cosineDistance, weights)
            }
            .sortedByDescending { it.matchScore }
            .map { recommendation ->
                CandidateJobRecommendationResponse(
                    jobId = requireNotNull(recommendation.job.id),
                    title = recommendation.job.title,
                    description = recommendation.job.description,
                    requirements = recommendation.job.requirements,
                    matchScore = recommendation.matchScore,
                    semanticScore = recommendation.semanticScore,
                    suitabilityReport = recommendation.suitabilityReport,
                    xaiReport = recommendation.xaiReport,
                    matchedSkills = recommendation.matchedSkills.sorted(),
                    missingSkills = recommendation.missingSkills.sorted(),
                    actionState = actionStateByJobId[requireNotNull(recommendation.job.id)] ?: JobActionStateResponse(false, false, false),
                    createdAt = recommendation.job.createdAt,
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
        if (!jobRepository.existsById(jobId)) {
            throw ResourceNotFoundException("Job not found")
        }

        return jobRecommendationRepository.findByJobId(jobId)
            .sortedByDescending { it.matchScore }
            .map { recommendation ->
                toRecommendationResponse(recommendation, parseTalentProfileOrNull(recommendation.resume))
            }
    }

    @Transactional(readOnly = true)
    fun listRecommendationsForResume(resumeId: UUID) = jobRecommendationRepository.findByResumeId(resumeId)

    private fun scoreResume(
        job: Job,
        resume: Resume,
        profile: TalentProfile,
        cosineDistance: BigDecimal,
        weights: AppliedEvaluationWeights,
    ): ScoredRecommendation {
        val skillBreakdown = calculateSkillScore(job, profile)
        val experienceBreakdown = calculateExperienceScore(job, profile)
        val educationBreakdown = calculateEducationScore(job, profile)
        val semanticScore = BigDecimal.valueOf((1.0 - cosineDistance.toDouble()).coerceIn(0.0, 1.0) * 100.0)
            .setScale(2, RoundingMode.HALF_UP)
        val hybridScore = BigDecimal.valueOf(
            skillBreakdown.score.toDouble() * weights.skillWeight.toDouble() / 100.0 +
                experienceBreakdown.score.toDouble() * weights.experienceWeight.toDouble() / 100.0 +
                educationBreakdown.score.toDouble() * weights.educationWeight.toDouble() / 100.0 +
                semanticScore.toDouble() * weights.semanticWeight.toDouble() / 100.0,
        )
            .setScale(2, RoundingMode.HALF_UP)

        val reasoning = buildString {
            append("Based on your requested weights - skills ")
            append(weights.skillWeight)
            append("%, experience ")
            append(weights.experienceWeight)
            append("%, education ")
            append(weights.educationWeight)
            append("%, semantic ")
            append(weights.semanticWeight)
            append("%. Hybrid score = skillScore(")
            append(skillBreakdown.score)
            append(") * ")
            append(weights.skillWeight)
            append("% + experienceScore(")
            append(experienceBreakdown.score)
            append(") * ")
            append(weights.experienceWeight)
            append("% + educationScore(")
            append(educationBreakdown.score)
            append(") * ")
            append(weights.educationWeight)
            append("% + semanticScore(")
            append(semanticScore)
            append(") * ")
            append(weights.semanticWeight)
            append(" = ")
            append(hybridScore)
            append(". Cosine distance: ")
            append(cosineDistance.setScale(4, RoundingMode.HALF_UP))
            append(". Matched skills ")
            append(skillBreakdown.matchedSkills.size)
            append(": ")
            append(skillBreakdown.matchedSkills.sorted().joinToString().ifBlank { "none" })
            append(". Missing: ")
            append(skillBreakdown.missingSkills.sorted().joinToString().ifBlank { "none" })
            append(". Experience years: ")
            append(experienceBreakdown.candidateYears)
            append(experienceBreakdown.requiredYears?.let { "/$it required" } ?: " (no explicit job requirement)")
            append(". Experience evidence: ")
            append(experienceBreakdown.matchedKeywords.sorted().joinToString().ifBlank { "none" })
            append(". Education evidence: ")
            append(educationBreakdown.matchedKeywords.sorted().joinToString().ifBlank { if (educationBreakdown.hasEducation) "baseline education present" else "none" })
            append(".")
        }
        val xaiReport = jobFitReportService.generate(
            JobFitReportRequest(
                audience = "hr",
                candidateName = profile.basicInfo.fullName,
                jobTitle = job.title,
                jobDescription = job.description,
                jobRequirements = job.requirements,
                matchScore = hybridScore,
                semanticScore = semanticScore,
                skillScore = skillBreakdown.score,
                experienceScore = experienceBreakdown.score,
                educationScore = educationBreakdown.score,
                matchedSkills = skillBreakdown.matchedSkills.sorted(),
                missingSkills = skillBreakdown.missingSkills.sorted(),
            ),
        )

        return ScoredRecommendation(
            resume = resume,
            profile = profile,
            skillScore = skillBreakdown.score,
            experienceScore = experienceBreakdown.score,
            educationScore = educationBreakdown.score,
            semanticScore = semanticScore,
            cosineDistance = cosineDistance.setScale(4, RoundingMode.HALF_UP),
            matchScore = hybridScore,
            xaiReasoning = reasoning,
            xaiReport = xaiReport,
        )
    }

    private fun calculateSkillScore(job: Job, profile: TalentProfile): SkillScoreBreakdown {
        val requiredSkills = extractRequiredSkillTerms(job)
        val jobKeywords = extractJobKeywords(job)
        val candidateSkillTerms = profile.skills
            .flatMap { tokenize(it.name) }
            .toSet()

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
    ): JobRecommendationResponse {
        val resume = recommendation.resume
        val basicInfo = profile?.basicInfo

        return JobRecommendationResponse(
            id = requireNotNull(recommendation.id),
            jobId = requireNotNull(recommendation.job.id),
            resumeId = requireNotNull(resume.id),
            matchScore = recommendation.matchScore,
            xaiReasoning = recommendation.xaiReasoning,
            xaiReport = recommendation.xaiReport?.let { objectMapper.convertValue(it, StructuredJobFitReport::class.java) },
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
                resumeRepository.findSemanticMatches(jobEmbedding)
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
                jobRepository.findSemanticMatches(candidateEmbedding)
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

    private fun scoreJobForCandidate(
        job: Job,
        profile: TalentProfile,
        cosineDistance: BigDecimal,
        weights: AppliedEvaluationWeights,
    ): ReverseScoredJobRecommendation {
        val skillBreakdown = calculateSkillScore(job, profile)
        val experienceBreakdown = calculateExperienceScore(job, profile)
        val educationBreakdown = calculateEducationScore(job, profile)
        val candidateSkillTerms = profile.skills.flatMap { tokenize(it.name) }.toSet()
        val requiredSkillLabels = extractRequiredSkillLabels(job)
        val matchedSkillLabels = requiredSkillLabels.filter { label ->
            tokenize(label).all { token -> candidateSkillTerms.contains(token) }
        }.toSet()
        val missingSkillLabels = requiredSkillLabels.filterNot { label ->
            tokenize(label).all { token -> candidateSkillTerms.contains(token) }
        }.toSet()
        val semanticScore = BigDecimal.valueOf((1.0 - cosineDistance.toDouble()).coerceIn(0.0, 1.0) * 100.0)
            .setScale(2, RoundingMode.HALF_UP)
        val matchScore = BigDecimal.valueOf(
            skillBreakdown.score.toDouble() * weights.skillWeight.toDouble() / 100.0 +
                experienceBreakdown.score.toDouble() * weights.experienceWeight.toDouble() / 100.0 +
                educationBreakdown.score.toDouble() * weights.educationWeight.toDouble() / 100.0 +
                semanticScore.toDouble() * weights.semanticWeight.toDouble() / 100.0,
        ).setScale(2, RoundingMode.HALF_UP)
        val xaiReport = jobFitReportService.generate(
            JobFitReportRequest(
                audience = "candidate",
                candidateName = profile.basicInfo.fullName,
                jobTitle = job.title,
                jobDescription = job.description,
                jobRequirements = job.requirements,
                matchScore = matchScore,
                semanticScore = semanticScore,
                skillScore = skillBreakdown.score,
                experienceScore = experienceBreakdown.score,
                educationScore = educationBreakdown.score,
                matchedSkills = matchedSkillLabels.sorted(),
                missingSkills = missingSkillLabels.sorted(),
            ),
        )

        val improvementAdvice = when {
            missingSkillLabels.isNotEmpty() -> {
                "如果优先补强 ${missingSkillLabels.sorted().take(3).joinToString("、")}，你的岗位竞争力会提升得更明显。"
            }

            experienceBreakdown.requiredYears != null && experienceBreakdown.candidateYears < experienceBreakdown.requiredYears -> {
                "继续积累与该岗位职责更贴近的项目经验，并把成果量化写进简历，会进一步提升成功率。"
            }

            !educationBreakdown.hasEducation -> {
                "建议补充教育背景或相关训练营/认证信息，让招聘方更容易判断你的基础能力。"
            }

            else -> {
                "你的核心能力已经比较贴合，下一步更适合强化项目成果、业务指标和技术深度证明。"
            }
        }

        val suitabilityReport = buildString {
            append(xaiReport.narrative)
            append(" ")
            append(improvementAdvice)
        }

        return ReverseScoredJobRecommendation(
            job = job,
            skillScore = skillBreakdown.score,
            experienceScore = experienceBreakdown.score,
            educationScore = educationBreakdown.score,
            semanticScore = semanticScore,
            matchScore = matchScore,
            matchedSkills = matchedSkillLabels.ifEmpty { skillBreakdown.matchedSkills },
            missingSkills = missingSkillLabels.ifEmpty { skillBreakdown.missingSkills },
            suitabilityReport = suitabilityReport,
            xaiReport = xaiReport,
        )
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

    private fun extractRequiredSkillTerms(job: Job): Set<String> {
        val skillsFromRequirements = when (val value = job.requirements?.get("skills")) {
            is Collection<*> -> value.filterIsInstance<String>().flatMap { tokenize(it) }
            is String -> tokenize(value)
            else -> emptyList()
        }

        if (skillsFromRequirements.isNotEmpty()) {
            return skillsFromRequirements.toSet()
        }

        return (tokenize(job.title) + tokenize(job.description)).toSet()
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
}