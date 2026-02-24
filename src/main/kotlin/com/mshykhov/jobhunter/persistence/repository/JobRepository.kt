package com.mshykhov.jobhunter.persistence.repository

import com.mshykhov.jobhunter.persistence.model.JobEntity
import com.mshykhov.jobhunter.persistence.model.JobSource
import com.mshykhov.jobhunter.persistence.model.JobStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface JobRepository : JpaRepository<JobEntity, UUID> {
    fun findByStatus(status: JobStatus): List<JobEntity>

    fun findBySource(source: JobSource): List<JobEntity>

    fun findByUrl(url: String): JobEntity?

    fun existsByUrl(url: String): Boolean
}
