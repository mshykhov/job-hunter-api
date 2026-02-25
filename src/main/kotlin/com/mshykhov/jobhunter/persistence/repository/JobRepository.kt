package com.mshykhov.jobhunter.persistence.repository

import com.mshykhov.jobhunter.persistence.model.JobEntity
import com.mshykhov.jobhunter.persistence.model.JobSource
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface JobRepository : JpaRepository<JobEntity, UUID> {
    fun findBySource(source: JobSource): List<JobEntity>

    fun findByUrl(url: String): JobEntity?

    fun findByUrlIn(urls: List<String>): List<JobEntity>

    fun findByMatchedAtIsNull(): List<JobEntity>

    fun existsByUrl(url: String): Boolean
}
