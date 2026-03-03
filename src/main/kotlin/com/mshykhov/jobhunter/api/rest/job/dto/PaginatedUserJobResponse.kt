package com.mshykhov.jobhunter.api.rest.job.dto

import com.mshykhov.jobhunter.application.userjob.UserJobStatus

data class PaginatedUserJobResponse(
    val content: List<UserJobResponse>,
    val totalElements: Long,
    val hasMore: Boolean,
    val size: Int,
    val statusCounts: Map<UserJobStatus, Long>,
)
