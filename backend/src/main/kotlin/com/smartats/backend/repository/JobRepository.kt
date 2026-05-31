package com.smartats.backend.repository

import com.smartats.backend.domain.Job
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface JobRepository : JpaRepository<Job, UUID> {
	fun findByOrganizationId(organizationId: UUID, pageable: Pageable): Page<Job>
	fun countByOrganizationId(organizationId: UUID): Long

	@Modifying
	@Query(
		value = "UPDATE jobs SET embedding = CAST(:embedding AS vector), updated_at = CURRENT_TIMESTAMP WHERE id = :jobId",
		nativeQuery = true,
	)
	fun updateEmbedding(@Param("jobId") jobId: UUID, @Param("embedding") embedding: String): Int

	@Query(
		value = """
			SELECT j.id AS jobId,
			       (j.embedding <=> CAST(:embedding AS vector)) AS cosineDistance
			FROM jobs j
			WHERE j.embedding IS NOT NULL
			ORDER BY j.embedding <=> CAST(:embedding AS vector) ASC
                        LIMIT :limit
		""",
		nativeQuery = true,
	)
	fun findSemanticMatches(@Param("embedding") embedding: String, @Param("limit") limit: Int = 100): List<JobSemanticMatchProjection>
}
