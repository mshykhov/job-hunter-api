package com.mshykhov.jobhunter.service

import com.mshykhov.jobhunter.controller.job.dto.JobIngestRequest
import com.mshykhov.jobhunter.controller.job.dto.JobResponse
import com.mshykhov.jobhunter.persistence.facade.JobFacade
import com.mshykhov.jobhunter.persistence.model.JobEntity
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val logger = KotlinLogging.logger {}

@Service
class JobService(
    private val jobFacade: JobFacade,
) {
    fun ingest(requests: List<JobIngestRequest>): List<JobResponse> =
        requests
            .map { request ->
                val existing = jobFacade.findByUrl(request.url)
                if (existing != null) {
                    updateExisting(existing, request)
                } else {
                    createNew(request)
                }
            }.map { JobResponse.from(it) }

    private fun createNew(request: JobIngestRequest): JobEntity =
        jobFacade.save(
            JobEntity(
                title = request.title,
                company = request.company,
                url = request.url,
                description = request.description,
                source = request.source,
                salary = request.salary,
                location = request.location,
                remote = request.remote,
                publishedAt = parsePublishedAt(request.publishedAt),
            ),
        )

    private fun updateExisting(
        entity: JobEntity,
        request: JobIngestRequest,
    ): JobEntity {
        entity.title = request.title
        entity.description = request.description
        entity.salary = request.salary
        entity.location = request.location
        entity.remote = request.remote
        entity.publishedAt = parsePublishedAt(request.publishedAt) ?: entity.publishedAt
        return jobFacade.save(entity)
    }

    private fun parsePublishedAt(raw: String?): Instant? {
        if (raw.isNullOrBlank()) return null
        return try {
            Instant.parse(raw)
        } catch (_: DateTimeParseException) {
            try {
                DateTimeFormatter.RFC_1123_DATE_TIME.parse(raw, Instant::from)
            } catch (_: DateTimeParseException) {
                logger.warn { "Failed to parse publishedAt: $raw" }
                null
            }
        }
    }
}
