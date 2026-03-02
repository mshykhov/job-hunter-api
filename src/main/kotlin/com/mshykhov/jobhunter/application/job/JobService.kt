package com.mshykhov.jobhunter.application.job

import com.mshykhov.jobhunter.api.rest.job.dto.JobIngestRequest
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
    fun ingest(requests: List<JobIngestRequest>): List<JobEntity> {
        val uniqueRequests = requests.associateBy { it.url }.values
        val urls = uniqueRequests.map { it.url }
        val existingByUrl = jobFacade.findByUrls(urls).associateBy { it.url }

        val toSave = mutableListOf<JobEntity>()
        val unchangedEntities = mutableListOf<JobEntity>()

        uniqueRequests.forEach { request ->
            val existing = existingByUrl[request.url]
            if (existing != null) {
                if (updateExisting(existing, request)) toSave.add(existing) else unchangedEntities.add(existing)
            } else {
                toSave.add(createNew(request))
            }
        }

        val newCount = toSave.count { it.isNew }
        val updatedCount = toSave.size - newCount
        val sources = (toSave + unchangedEntities).groupingBy { it.source }.eachCount()
        logger.info {
            "Ingest: ${toSave.size + unchangedEntities.size} jobs ($newCount new, $updatedCount updated, ${unchangedEntities.size} unchanged), sources: $sources"
        }

        return jobFacade.saveAll(toSave) + unchangedEntities
    }

    private fun createNew(request: JobIngestRequest): JobEntity =
        JobEntity(
            title = request.title,
            company = request.company,
            url = request.url,
            description = request.description,
            source = request.source,
            rawData = request.rawData,
            salary = request.salary,
            location = request.location,
            remote = request.remote,
            publishedAt = parsePublishedAt(request.publishedAt),
        )

    /** Returns true if any field changed, false if the job is identical to what we already have. */
    private fun updateExisting(
        entity: JobEntity,
        request: JobIngestRequest,
    ): Boolean {
        val parsedPublishedAt = parsePublishedAt(request.publishedAt)
        val hasChanges =
            entity.title != request.title ||
                entity.description != request.description ||
                entity.salary != request.salary ||
                entity.location != request.location ||
                entity.remote != request.remote ||
                entity.rawData != request.rawData ||
                (parsedPublishedAt != null && entity.publishedAt != parsedPublishedAt)
        if (!hasChanges) return false

        entity.title = request.title
        entity.description = request.description
        entity.rawData = request.rawData
        entity.salary = request.salary
        entity.location = request.location
        entity.remote = request.remote
        entity.publishedAt = parsedPublishedAt ?: entity.publishedAt
        return true
    }

    fun findExistingUrls(urls: List<String>): List<String> {
        if (urls.isEmpty()) return emptyList()
        return jobFacade.findByUrls(urls).map { it.url }
    }

    private fun parsePublishedAt(raw: String?): Instant? {
        val parsed = DateTimeParser.toInstant(raw)
        if (parsed == null && !raw.isNullOrBlank()) {
            logger.warn { "Failed to parse publishedAt: $raw" }
        }
        return parsed
    }
}
