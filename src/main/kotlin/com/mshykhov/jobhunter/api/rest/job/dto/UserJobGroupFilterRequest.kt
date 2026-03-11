package com.mshykhov.jobhunter.api.rest.job.dto

import com.mshykhov.jobhunter.application.userjob.UserJobGroupSort
import com.mshykhov.jobhunter.application.userjob.UserJobStatus
import java.time.Instant

data class UserJobGroupFilterRequest(
    val statuses: List<UserJobStatus>? = null,
    val matchedAfter: Instant? = null,
    val search: String? = null,
    val remote: Boolean? = null,
    val minScore: Int? = null,
    val page: Int = 0,
    val size: Int = 50,
    val sortBy: UserJobGroupSort = UserJobGroupSort.SCORE,
)
