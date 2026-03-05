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
    val remote: Boolean?,
    val status: UserJobStatus,
    val aiRelevanceScore: Int,
    val aiReasoning: String,
    val coverLetter: String?,
    val recruiterMessage: String?,
    val publishedAt: Instant?,
    val matchedAt: Instant?,
    val updatedAt: Instant?,
) {
    companion object {
        fun from(entity: UserJobEntity): UserJobDetailResponse {
            val base = UserJobResponse.from(entity)
            return UserJobDetailResponse(
                id = base.id,
                jobId = base.jobId,
                title = base.title,
                company = base.company,
                url = base.url,
                description = entity.job.description,
                source = base.source,
                salary = base.salary,
                location = base.location,
                remote = base.remote,
                status = base.status,
                aiRelevanceScore = base.aiRelevanceScore,
                aiReasoning = entity.aiReasoning,
                coverLetter = entity.coverLetter,
                recruiterMessage = entity.recruiterMessage,
                publishedAt = base.publishedAt,
                matchedAt = base.matchedAt,
                updatedAt = base.updatedAt,
            )
        }
    }
}
