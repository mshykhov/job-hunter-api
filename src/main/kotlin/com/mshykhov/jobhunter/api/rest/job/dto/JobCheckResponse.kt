package com.mshykhov.jobhunter.api.rest.job.dto

data class JobCheckResponse(
    val newUrls: List<String>,
    val updatedUrls: List<String>,
    val unchangedUrls: List<String>,
)
