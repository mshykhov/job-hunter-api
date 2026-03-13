package com.mshykhov.jobhunter.api.rest.job.dto

import com.mshykhov.jobhunter.application.job.Category
import com.mshykhov.jobhunter.application.userjob.UserJobGroupEntity
import com.mshykhov.jobhunter.application.userjob.UserJobStatus
import java.util.UUID

data class JobGroupDetailResponse(
    val groupId: UUID,
    val title: String,
    val company: String?,
    val status: UserJobStatus,
    val aiRelevanceScore: Int,
    val aiReasoning: String,
    val categories: Set<Category>,
    val remote: Boolean,
    val jobs: List<GroupJobResponse>,
) {
    companion object {
        fun from(
            userGroup: UserJobGroupEntity,
            jobs: List<GroupJobResponse>,
        ): JobGroupDetailResponse =
            JobGroupDetailResponse(
                groupId = userGroup.group.id,
                title = userGroup.group.title,
                company = userGroup.group.company,
                status = userGroup.status,
                aiRelevanceScore = userGroup.aiRelevanceScore,
                aiReasoning = userGroup.aiReasoning,
                categories = userGroup.group.categories,
                remote = jobs.any { it.remote == true },
                jobs = jobs,
            )
    }
}
