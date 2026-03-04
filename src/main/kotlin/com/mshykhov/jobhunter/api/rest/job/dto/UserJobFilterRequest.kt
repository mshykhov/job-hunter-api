package com.mshykhov.jobhunter.api.rest.job.dto

import com.mshykhov.jobhunter.application.job.JobSource
import com.mshykhov.jobhunter.application.userjob.UserJobStatus
import java.time.Instant
import java.util.UUID

data class UserJobFilterRequest(
    val statuses: List<UserJobStatus>? = null,
    val sources: List<JobSource>? = null,
    val publishedAfter: Instant? = null,
    val matchedAfter: Instant? = null,
    val updatedAfter: Instant? = null,
    val search: String? = null,
    val remote: Boolean? = null,
    val size: Int = 50,
    val cursorCreatedAt: Instant? = null,
    val cursorId: UUID? = null,
)
