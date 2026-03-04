package com.mshykhov.jobhunter.api.rest.job.dto

import com.mshykhov.jobhunter.application.userjob.UserJobStatus

data class PaginatedUserJobResponse(
    val content: List<UserJobResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val statusCounts: Map<UserJobStatus, Long>,
)
