package com.mshykhov.jobhunter.application.job

import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

interface JobRepository : JpaRepository<JobEntity, UUID> {
    fun findByUrlIn(urls: List<String>): List<JobEntity>

    fun findByMatchedAtIsNull(): List<JobEntity>

    fun findByMatchedAtIsNotNull(): List<JobEntity>

    fun findByMatchedAtGreaterThanEqual(since: Instant): List<JobEntity>
}
