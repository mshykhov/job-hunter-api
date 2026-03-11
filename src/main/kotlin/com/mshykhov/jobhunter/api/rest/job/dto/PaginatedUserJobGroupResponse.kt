package com.mshykhov.jobhunter.api.rest.job.dto

import com.mshykhov.jobhunter.application.userjob.UserJobStatus

data class PaginatedUserJobGroupResponse(
    val content: List<UserJobGroupResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val statusCounts: Map<UserJobStatus, Long>,
)
