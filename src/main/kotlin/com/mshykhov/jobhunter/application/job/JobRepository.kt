package com.mshykhov.jobhunter.application.job

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

    fun findByMatchedAtIsNull(): List<JobEntity>

    fun findByMatchedAtIsNotNull(): List<JobEntity>

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
}
