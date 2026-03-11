package com.mshykhov.jobhunter.api.rest.job.dto

import com.mshykhov.jobhunter.application.job.JobEntity
import com.mshykhov.jobhunter.application.job.JobSource
import com.mshykhov.jobhunter.application.userjob.UserJobEntity
import java.time.Instant
import java.util.UUID

data class GroupJobResponse(
    val jobId: UUID,
    val url: String,
    val source: JobSource,
    val description: String,
    val salary: String?,
    val location: String?,
    val remote: Boolean?,
    val coverLetter: String?,
    val recruiterMessage: String?,
    val publishedAt: Instant?,
    val scrapedAt: Instant?,
) {
    companion object {
        fun from(
            job: JobEntity,
            userJob: UserJobEntity? = null,
        ): GroupJobResponse =
            GroupJobResponse(
                jobId = job.id,
                url = job.url,
                source = job.source,
                description = job.description,
                salary = job.salary,
                location = job.location,
                remote = job.remote,
                coverLetter = userJob?.coverLetter,
                recruiterMessage = userJob?.recruiterMessage,
                publishedAt = job.publishedAt,
                scrapedAt = job.updatedAt,
            )
    }
}
