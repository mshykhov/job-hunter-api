package com.mshykhov.jobhunter.application.job.dto

import com.mshykhov.jobhunter.application.job.JobSource
import jakarta.validation.constraints.NotBlank

data class JobIngestRequest(
    @field:NotBlank
    val title: String,
    val company: String? = null,
    @field:NotBlank
    val url: String,
    val description: String = "",
    val source: JobSource,
    val salary: String? = null,
    val location: String? = null,
    val remote: Boolean? = null,
    val publishedAt: String? = null,
    val rawData: Map<String, Any?> = emptyMap(),
)
