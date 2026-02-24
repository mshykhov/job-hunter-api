package com.mshykhov.jobhunter.model.dto

import com.mshykhov.jobhunter.persistence.model.JobSource
import com.mshykhov.jobhunter.persistence.model.JobStatus
import java.time.Instant
import java.util.UUID

data class JobResponse(
    val id: UUID,
    val title: String,
    val company: String,
    val url: String,
    val description: String,
    val source: JobSource,
    val salary: String?,
    val location: String?,
    val remote: Boolean,
    val status: JobStatus,
    val publishedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
