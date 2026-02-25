package com.mshykhov.jobhunter.service

import com.mshykhov.jobhunter.controller.job.dto.JobIngestRequest
import com.mshykhov.jobhunter.controller.job.dto.JobResponse
import com.mshykhov.jobhunter.persistence.facade.JobFacade
import com.mshykhov.jobhunter.persistence.model.JobEntity
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException


private val logger = KotlinLogging.logger {}

private val INSTANT_PARSERS: List<(String) -> Instant?> = listOf(
    { raw -> try { Instant.parse(raw) } catch (_: DateTimeParseException) { null } },
    { raw -> try { DateTimeFormatter.RFC_1123_DATE_TIME.parse(raw, Instant::from) } catch (_: DateTimeParseException) { null } },
    { raw -> try { LocalDateTime.parse(raw).toInstant(ZoneOffset.UTC) } catch (_: DateTimeParseException) { null } },
)

@Service
class JobService(
    private val jobFacade: JobFacade,
) {
    @Transactional
    fun ingest(requests: List<JobIngestRequest>): List<JobResponse> {
        val urls = requests.map { it.url }
        val existingByUrl = jobFacade.findByUrls(urls).associateBy { it.url }

        val entities =
            requests.map { request ->
                val existing = existingByUrl[request.url]
                if (existing != null) {
                    updateExisting(existing, request)
                } else {
                    createNew(request)
                }
            }

        return jobFacade.saveAll(entities).map { JobResponse.from(it) }
    }

    private fun createNew(request: JobIngestRequest): JobEntity =
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
        return entity
    }

    private fun parsePublishedAt(raw: String?): Instant? {
        if (raw.isNullOrBlank()) return null
        return INSTANT_PARSERS.firstNotNullOfOrNull { it(raw) }
            .also { if (it == null) logger.warn { "Failed to parse publishedAt: $raw" } }
    }
}
