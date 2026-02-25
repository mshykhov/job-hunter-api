package com.mshykhov.jobhunter.application.job

import com.mshykhov.jobhunter.api.rest.job.dto.JobIngestRequest
import com.mshykhov.jobhunter.api.rest.job.dto.JobResponse
import com.mshykhov.jobhunter.application.common.DateTimeParser
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

private val logger = KotlinLogging.logger {}

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
        val parsed = DateTimeParser.toInstant(raw)
        if (parsed == null && !raw.isNullOrBlank()) {
            logger.warn { "Failed to parse publishedAt: $raw" }
        }
        return parsed
    }
}
