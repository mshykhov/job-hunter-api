package com.mshykhov.jobhunter.model.dto

import jakarta.validation.constraints.NotBlank

data class JobIngestRequest(
    @field:NotBlank
    val title: String,
    @field:NotBlank
    val company: String,
    @field:NotBlank
    val url: String,
    val description: String? = null,
    @field:NotBlank
    val source: String,
    val salary: String? = null,
    val location: String? = null,
    val remote: Boolean = false,
)
