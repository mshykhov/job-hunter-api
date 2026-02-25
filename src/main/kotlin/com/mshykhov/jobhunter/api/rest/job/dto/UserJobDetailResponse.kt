package com.mshykhov.jobhunter.api.rest.job.dto

import com.mshykhov.jobhunter.application.job.JobSource
import com.mshykhov.jobhunter.application.userjob.UserJobEntity
import com.mshykhov.jobhunter.application.userjob.UserJobStatus
import java.time.Instant
import java.util.UUID

data class UserJobDetailResponse(
    val id: UUID,
    val jobId: UUID,
    val title: String,
    val company: String?,
    val url: String,
    val description: String,
    val source: JobSource,
    val salary: String?,
    val location: String?,
    val remote: Boolean,
    val status: UserJobStatus,
    val aiRelevanceScore: Int,
    val aiReasoning: String,
    val publishedAt: Instant?,
    val matchedAt: Instant?,
) {
    companion object {
        fun from(entity: UserJobEntity): UserJobDetailResponse =
            UserJobDetailResponse(
                id = entity.id,
                jobId = entity.job.id,
                title = entity.job.title,
                company = entity.job.company,
                url = entity.job.url,
                description = entity.job.description,
                source = entity.job.source,
                salary = entity.job.salary,
                location = entity.job.location,
                remote = entity.job.remote,
                status = entity.status,
                aiRelevanceScore = entity.aiRelevanceScore,
                aiReasoning = entity.aiReasoning,
                publishedAt = entity.job.publishedAt,
                matchedAt = entity.createdAt,
            )
    }
}
