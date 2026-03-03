package com.mshykhov.jobhunter.api.rest.job.dto

data class PublicJobPageResponse(
    val content: List<PublicJobResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
