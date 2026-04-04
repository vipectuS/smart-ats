package com.smartats.backend

import com.smartats.backend.domain.Job
import com.smartats.backend.domain.JobApplication
import com.smartats.backend.domain.JobApplicationStatus
import com.smartats.backend.domain.JobFavorite
import com.smartats.backend.domain.JobFavoriteStatus
import com.smartats.backend.domain.JobIgnore
import com.smartats.backend.domain.JobIgnoreStatus
import com.smartats.backend.domain.User
import com.smartats.backend.domain.UserRole
import com.smartats.backend.dto.candidate.CandidateTimelineAction
import com.smartats.backend.repository.JobApplicationRepository
import com.smartats.backend.repository.JobFavoriteRepository
import com.smartats.backend.repository.JobIgnoreRepository
import com.smartats.backend.repository.JobRepository
import com.smartats.backend.repository.UserRepository
import com.smartats.backend.service.CandidateJobActionService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

class CandidateJobActionServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var jobRepository: JobRepository
    private lateinit var jobApplicationRepository: JobApplicationRepository
    private lateinit var jobFavoriteRepository: JobFavoriteRepository
    private lateinit var jobIgnoreRepository: JobIgnoreRepository

    private lateinit var service: CandidateJobActionService

    @BeforeEach
    fun setUp() {
        userRepository = mock(UserRepository::class.java)
        jobRepository = mock(JobRepository::class.java)
        jobApplicationRepository = mock(JobApplicationRepository::class.java)
        jobFavoriteRepository = mock(JobFavoriteRepository::class.java)
        jobIgnoreRepository = mock(JobIgnoreRepository::class.java)

        service = CandidateJobActionService(
            userRepository = userRepository,
            jobRepository = jobRepository,
            jobApplicationRepository = jobApplicationRepository,
            jobFavoriteRepository = jobFavoriteRepository,
            jobIgnoreRepository = jobIgnoreRepository,
        )
    }

    @Test
    fun `timeline aggregates records from three tables in descending timestamp order`() {
        val candidateId = UUID.randomUUID()
        val candidate = User(
            id = candidateId,
            username = "timeline_candidate",
            passwordHash = "hash",
            email = "timeline_candidate@example.com",
            role = UserRole.CANDIDATE,
        )
        val hrOwner = User(
            id = UUID.randomUUID(),
            username = "acme_hr",
            passwordHash = "hash",
            email = "acme_hr@example.com",
            role = UserRole.HR,
        )

        val appliedJob = Job(id = UUID.randomUUID(), title = "Backend Engineer", description = "", createdBy = hrOwner)
        val favoriteJob = Job(id = UUID.randomUUID(), title = "Platform Engineer", description = "", createdBy = hrOwner)
        val ignoredJob = Job(id = UUID.randomUUID(), title = "Frontend Engineer", description = "", createdBy = hrOwner)

        val application = JobApplication(
            id = UUID.randomUUID(),
            user = candidate,
            job = appliedJob,
            status = JobApplicationStatus.WITHDRAWN,
            createdAt = LocalDateTime.of(2026, 4, 4, 8, 0, 0),
            updatedAt = LocalDateTime.of(2026, 4, 4, 10, 0, 0),
        )
        val favorite = JobFavorite(
            id = UUID.randomUUID(),
            user = candidate,
            job = favoriteJob,
            status = JobFavoriteStatus.FAVORITED,
            createdAt = LocalDateTime.of(2026, 4, 4, 7, 0, 0),
            updatedAt = LocalDateTime.of(2026, 4, 4, 11, 0, 0),
        )
        val ignore = JobIgnore(
            id = UUID.randomUUID(),
            user = candidate,
            job = ignoredJob,
            status = JobIgnoreStatus.UNIGNORED,
            createdAt = LocalDateTime.of(2026, 4, 4, 6, 0, 0),
            updatedAt = LocalDateTime.of(2026, 4, 4, 9, 0, 0),
        )

        given(userRepository.findByUsername("timeline_candidate")).willReturn(Optional.of(candidate))
        given(jobApplicationRepository.findByUserIdOrderByUpdatedAtDesc(candidateId)).willReturn(listOf(application))
        given(jobFavoriteRepository.findByUserIdOrderByUpdatedAtDesc(candidateId)).willReturn(listOf(favorite))
        given(jobIgnoreRepository.findByUserIdOrderByUpdatedAtDesc(candidateId)).willReturn(listOf(ignore))

        val timeline = service.listTimeline("timeline_candidate")

        assertEquals(
            listOf(
                CandidateTimelineAction.FAVORITED,
                CandidateTimelineAction.WITHDRAWN,
                CandidateTimelineAction.UNIGNORED,
            ),
            timeline.map { it.action },
        )
        assertEquals("Platform Engineer", timeline[0].jobTitle)
        assertEquals("acme_hr", timeline[0].companyName)
        assertEquals("2026-04-04T11:00:00Z", timeline[0].timestamp.toString())
    }

    @Test
    fun `timeline returns empty list when candidate has no action records`() {
        val candidateId = UUID.randomUUID()
        val candidate = User(
            id = candidateId,
            username = "empty_candidate",
            passwordHash = "hash",
            email = "empty_candidate@example.com",
            role = UserRole.CANDIDATE,
        )

        given(userRepository.findByUsername("empty_candidate")).willReturn(Optional.of(candidate))
        given(jobApplicationRepository.findByUserIdOrderByUpdatedAtDesc(candidateId)).willReturn(emptyList())
        given(jobFavoriteRepository.findByUserIdOrderByUpdatedAtDesc(candidateId)).willReturn(emptyList())
        given(jobIgnoreRepository.findByUserIdOrderByUpdatedAtDesc(candidateId)).willReturn(emptyList())

        assertEquals(emptyList<Any>(), service.listTimeline("empty_candidate"))
    }
}