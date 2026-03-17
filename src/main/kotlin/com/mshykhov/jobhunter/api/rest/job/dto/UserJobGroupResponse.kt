package com.mshykhov.jobhunter.api.rest.job.dto

import com.mshykhov.jobhunter.application.job.Category
import com.mshykhov.jobhunter.application.job.JobSource
import com.mshykhov.jobhunter.application.userjob.UserJobGroupEntity
import com.mshykhov.jobhunter.application.userjob.UserJobStatus
import java.time.Instant
import java.util.UUID

data class UserJobGroupResponse(
    val id: UUID,
    val groupId: UUID,
    val title: String,
    val company: String?,
    val status: UserJobStatus,
    val aiRelevanceScore: Int,
    val jobCount: Int,
    val categories: Set<Category>,
    val sources: List<JobSource>,
    val locations: List<String>,
    val remote: Boolean,
    val salary: String?,
    val publishedAt: Instant?,
    val matchedAt: Instant?,
    val createdAt: Instant?,
    val updatedAt: Instant?,
) {
    companion object {
        fun from(entity: UserJobGroupEntity): UserJobGroupResponse {
            val jobs = entity.group.jobs
            return UserJobGroupResponse(
                id = entity.id,
                groupId = entity.group.id,
                title = entity.group.title,
                company = entity.group.company,
                status = entity.status,
                aiRelevanceScore = entity.aiRelevanceScore,
                jobCount = jobs.size,
                categories = entity.group.categories,
                sources = jobs.map { it.source }.distinct().sortedBy { it.value },
                locations = jobs.mapNotNull { it.location }.distinct().sorted(),
                remote = jobs.any { it.remote == true },
                salary = jobs.firstNotNullOfOrNull { it.salary },
                publishedAt = jobs.mapNotNull { it.publishedAt }.minOrNull(),
                matchedAt = entity.createdAt,
                createdAt = entity.group.createdAt,
                updatedAt = entity.group.updatedAt,
            )
        }
    }
}
