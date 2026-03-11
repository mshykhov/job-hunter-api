package com.mshykhov.jobhunter.api.rest.job.dto

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
    val remote: Boolean,
    val jobs: List<GroupJobResponse>,
) {
    companion object {
        fun from(
            group: UserJobGroupEntity,
            jobs: List<GroupJobResponse>,
        ): JobGroupDetailResponse =
            JobGroupDetailResponse(
                groupId = group.group.id,
                title = group.group.title,
                company = group.group.company,
                status = group.status,
                aiRelevanceScore = group.aiRelevanceScore,
                aiReasoning = group.aiReasoning,
                remote = jobs.any { it.remote == true },
                jobs = jobs,
            )
    }
}
