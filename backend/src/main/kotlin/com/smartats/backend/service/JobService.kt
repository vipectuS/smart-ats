package com.smartats.backend.service

import com.smartats.backend.domain.Job
import com.smartats.backend.dto.PageResponse
import com.smartats.backend.dto.job.CreateJobRequest
import com.smartats.backend.dto.job.JobResponse
import com.smartats.backend.exception.ResourceNotFoundException
import com.smartats.backend.repository.JobRepository
import com.smartats.backend.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class JobService(
    private val jobRepository: JobRepository,
    private val userRepository: UserRepository,
    private val embeddingService: EmbeddingService,
) {

    @Transactional
    fun createJob(username: String, request: CreateJobRequest): JobResponse {
        val creator = userRepository.findByUsername(username)
            .orElseThrow { ResourceNotFoundException("User not found") }

        val job = Job(
            title = request.title.trim(),
            description = request.description.trim(),
            requirements = request.requirements,
            createdBy = creator,
        )

        val savedJob = jobRepository.save(job)
        val embedding = embeddingService.generateJobEmbedding(savedJob)
        persistJobEmbedding(savedJob, embedding)
        return JobResponse.from(savedJob)
    }

    @Transactional(readOnly = true)
    fun getJob(jobId: UUID): JobResponse {
        val job = jobRepository.findById(jobId)
            .orElseThrow { ResourceNotFoundException("Job not found") }
        return JobResponse.from(job)
    }

    @Transactional(readOnly = true)
    fun listJobs(page: Int, size: Int): PageResponse<JobResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val jobs = jobRepository.findAll(pageable).map(JobResponse::from)
        return PageResponse.from(jobs)
    }

    private fun persistJobEmbedding(job: Job, embedding: String) {
        val jobId = requireNotNull(job.id)
        if (embeddingService.shouldUseNativeVectorStorage()) {
            jobRepository.updateEmbedding(jobId, embedding)
        } else {
            job.embedding = embedding
            jobRepository.save(job)
        }
        job.embedding = embedding
    }
}