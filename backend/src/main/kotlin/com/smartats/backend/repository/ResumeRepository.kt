package com.smartats.backend.repository

import com.smartats.backend.domain.Resume
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface ResumeRepository : JpaRepository<Resume, UUID> {
	fun findTopByOwnerUserIdOrderByUpdatedAtDesc(userId: UUID): Resume?

	fun findTopByOwnerUserIdAndStatusOrderByUpdatedAtDesc(userId: UUID, status: String): Resume?

	fun findByStatus(status: String): List<Resume>

	fun findByStatusOrderByUpdatedAtDesc(status: String, pageable: Pageable): List<Resume>

	fun findByStatusAndUpdatedAtGreaterThanEqual(status: String, updatedAt: LocalDateTime): List<Resume>

	fun countByCreatedAtGreaterThanEqual(createdAt: LocalDateTime): Long

	fun countByStatus(status: String): Long

	fun countByStatusAndUpdatedAtGreaterThanEqual(status: String, updatedAt: LocalDateTime): Long

	@Query(
		value = """
			SELECT CAST(r.created_at AS DATE) AS day,
			       COUNT(*) AS total
			FROM resumes r
			WHERE CAST(r.created_at AS DATE) BETWEEN :startDate AND :endDate
			GROUP BY CAST(r.created_at AS DATE)
			ORDER BY day ASC
		""",
		nativeQuery = true,
	)
	fun countReceivedByDayBetween(
		@Param("startDate") startDate: LocalDate,
		@Param("endDate") endDate: LocalDate,
	): List<DailyCountProjection>

	@Query(
		value = """
			SELECT CAST(r.updated_at AS DATE) AS day,
			       COUNT(*) AS total
			FROM resumes r
			WHERE r.status = :status
			  AND CAST(r.updated_at AS DATE) BETWEEN :startDate AND :endDate
			GROUP BY CAST(r.updated_at AS DATE)
			ORDER BY day ASC
		""",
		nativeQuery = true,
	)
	fun countParsedByDayBetween(
		@Param("status") status: String,
		@Param("startDate") startDate: LocalDate,
		@Param("endDate") endDate: LocalDate,
	): List<DailyCountProjection>

	@Modifying
	@Query(
		value = "UPDATE resumes SET embedding = CAST(:embedding AS vector), updated_at = CURRENT_TIMESTAMP WHERE id = :resumeId",
		nativeQuery = true,
	)
	fun updateEmbedding(@Param("resumeId") resumeId: UUID, @Param("embedding") embedding: String): Int

	@Query(
		value = """
			SELECT r.id AS resumeId,
				   (r.embedding <=> CAST(:embedding AS vector)) AS cosineDistance
			FROM resumes r
			WHERE r.status = 'PARSED'
			  AND r.embedding IS NOT NULL
			ORDER BY r.embedding <=> CAST(:embedding AS vector) ASC
		""",
		nativeQuery = true,
	)
	fun findSemanticMatches(@Param("embedding") embedding: String): List<ResumeSemanticMatchProjection>
}
