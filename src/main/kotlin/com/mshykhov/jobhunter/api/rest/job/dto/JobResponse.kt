package com.mshykhov.jobhunter.api.rest.job.dto

import com.mshykhov.jobhunter.application.job.JobEntity
import com.mshykhov.jobhunter.application.job.JobSource
import java.time.Instant
import java.util.UUID

data class JobResponse(
    val id: UUID,
    val title: String,
    val company: String?,
    val url: String,
    val description: String,
    val source: JobSource,
    val salary: String?,
    val location: String?,
    val remote: Boolean,
    val publishedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(entity: JobEntity): JobResponse =
            JobResponse(
                id = entity.id,
                title = entity.title,
                company = entity.company,
                url = entity.url,
                description = entity.description,
                source = entity.source,
                salary = entity.salary,
                location = entity.location,
                remote = entity.remote,
                publishedAt = entity.publishedAt,
                createdAt = entity.createdAt!!,
                updatedAt = entity.updatedAt!!,
            )
    }
}
