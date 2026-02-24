package com.mshykhov.jobhunter.model.dto

import com.mshykhov.jobhunter.persistence.model.JobSource
import jakarta.validation.constraints.NotBlank
import java.time.Instant

data class JobIngestRequest(
    @field:NotBlank
    val title: String,
    @field:NotBlank
    val company: String,
    @field:NotBlank
    val url: String,
    @field:NotBlank
    val description: String,
    val source: JobSource,
    val salary: String? = null,
    val location: String? = null,
    val remote: Boolean = false,
    val publishedAt: Instant? = null,
)
