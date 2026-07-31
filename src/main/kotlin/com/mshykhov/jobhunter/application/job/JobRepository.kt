package com.mshykhov.jobhunter.application.job

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.UUID

interface JobRepository :
    JpaRepository<JobEntity, UUID>,
    JpaSpecificationExecutor<JobEntity> {
    fun findByUrlIn(urls: List<String>): List<JobEntity>

    fun findByGroupId(groupId: UUID): List<JobEntity>

    @EntityGraph(attributePaths = ["group"])
    fun findByGroupIdInAndMatchedAtIsNullAndMatchAttemptsLessThan(
        groupIds: List<UUID>,
        maxAttempts: Int,
        pageable: Pageable,
    ): List<JobEntity>

    @EntityGraph(attributePaths = ["group"])
    fun findByMatchedAtIsNullAndMatchAttemptsLessThan(
        maxAttempts: Int,
        pageable: Pageable,
    ): List<JobEntity>

    fun findByMatchedAtIsNotNull(): List<JobEntity>

    fun countByMatchedAtIsNull(): Long

    fun findByMatchedAtGreaterThanEqual(since: Instant): List<JobEntity>

    fun findTopBySourceOrderByCreatedAtDesc(source: JobSource): JobEntity?

    @Modifying
    @Query("UPDATE JobEntity j SET j.matchedAt = :matchedAt WHERE j.id IN :ids")
    fun updateMatchedAt(
        ids: List<UUID>,
        matchedAt: Instant?,
    )

    @Modifying
    @Query("UPDATE JobEntity j SET j.remote = :remote WHERE j.id = :id")
    fun updateRemote(
        id: UUID,
        remote: Boolean,
    )

    @Modifying
    @Query("UPDATE JobEntity j SET j.matchAttempts = j.matchAttempts + 1 WHERE j.id IN :ids")
    fun incrementMatchAttempts(ids: List<UUID>)

    @Modifying
    @Query("UPDATE JobEntity j SET j.lastSeenAt = :seenAt WHERE j.id IN :ids")
    fun updateLastSeenAt(
        ids: List<UUID>,
        seenAt: Instant,
    )

    @Query(
        """
        SELECT j.id FROM JobEntity j
        WHERE j.lastSeenAt < :threshold
          AND j.matchedAt IS NOT NULL
          AND NOT EXISTS (SELECT 1 FROM UserJobGroupEntity u WHERE u.group.id = j.group.id)
          AND NOT EXISTS (SELECT 1 FROM UserJobEntity uj WHERE uj.job.id = j.id)
        ORDER BY j.lastSeenAt ASC
        """,
    )
    fun findPurgeableIds(
        threshold: Instant,
        pageable: Pageable,
    ): List<UUID>

    @Query("SELECT DISTINCT j.group.id FROM JobEntity j WHERE j.id IN :ids")
    fun findGroupIdsByIds(ids: List<UUID>): List<UUID>

    @Modifying
    @Query("DELETE FROM JobEntity j WHERE j.id IN :ids")
    fun deleteByIds(ids: List<UUID>)
}
