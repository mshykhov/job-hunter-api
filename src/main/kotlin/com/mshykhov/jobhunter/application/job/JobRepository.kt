package com.mshykhov.jobhunter.application.job

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.UUID

interface JobRepository : JpaRepository<JobEntity, UUID> {
    fun findByUrlIn(urls: List<String>): List<JobEntity>

    fun findByMatchedAtIsNull(): List<JobEntity>

    fun findByMatchedAtIsNotNull(): List<JobEntity>

    fun findByMatchedAtGreaterThanEqual(since: Instant): List<JobEntity>

    @Modifying
    @Query("UPDATE JobEntity j SET j.matchedAt = :matchedAt WHERE j.id IN :ids")
    fun updateMatchedAt(
        ids: List<UUID>,
        matchedAt: Instant?,
    )
}
