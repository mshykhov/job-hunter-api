package com.mshykhov.jobhunter.controller.job.dto

import com.mshykhov.jobhunter.persistence.model.JobSource
import jakarta.validation.constraints.NotBlank

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
    val publishedAt: String? = null,
)
