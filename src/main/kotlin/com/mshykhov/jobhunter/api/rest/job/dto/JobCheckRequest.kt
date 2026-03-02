package com.mshykhov.jobhunter.api.rest.job.dto

import jakarta.validation.constraints.NotBlank

data class JobCheckRequest(
    @field:NotBlank
    val url: String,
    val title: String? = null,
    val company: String? = null,
    val salary: String? = null,
    val location: String? = null,
    val publishedAt: String? = null,
)
