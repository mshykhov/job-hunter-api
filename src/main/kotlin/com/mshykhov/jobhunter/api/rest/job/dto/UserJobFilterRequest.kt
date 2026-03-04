package com.mshykhov.jobhunter.api.rest.job.dto

import com.mshykhov.jobhunter.application.job.JobSource
import com.mshykhov.jobhunter.application.userjob.UserJobSort
import com.mshykhov.jobhunter.application.userjob.UserJobStatus
import java.time.Instant

data class UserJobFilterRequest(
    val statuses: List<UserJobStatus>? = null,
    val sources: List<JobSource>? = null,
    val publishedAfter: Instant? = null,
    val matchedAfter: Instant? = null,
    val updatedAfter: Instant? = null,
    val search: String? = null,
    val remote: Boolean? = null,
    val minScore: Int? = null,
    val page: Int = 0,
    val size: Int = 50,
    val sortBy: UserJobSort = UserJobSort.SCORE,
)
