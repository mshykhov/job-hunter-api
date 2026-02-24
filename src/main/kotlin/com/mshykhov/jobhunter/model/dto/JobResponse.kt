package com.mshykhov.jobhunter.model.dto

import java.time.Instant
import java.util.UUID

data class JobResponse(
    val id: UUID,
    val title: String,
    val company: String,
    val url: String,
    val description: String?,
    val source: String,
    val salary: String?,
    val location: String?,
    val remote: Boolean,
    val status: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)
