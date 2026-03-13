package com.mshykhov.jobhunter.api.rest.job.dto

import com.mshykhov.jobhunter.application.job.Category
import com.mshykhov.jobhunter.application.job.JobEntity
import com.mshykhov.jobhunter.application.job.JobGroupEntity
import com.mshykhov.jobhunter.application.job.JobSource
import jakarta.validation.constraints.NotBlank
import java.time.Instant

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
    val category: Category,
) {
    fun toEntity(
        parsedPublishedAt: Instant?,
        group: JobGroupEntity,
    ): JobEntity =
        JobEntity(
            title = title,
            company = company,
            group = group,
            url = url,
            description = description,
            source = source,
            rawData = rawData,
            salary = salary,
            location = location,
            remote = remote,
            publishedAt = parsedPublishedAt,
        )
}
