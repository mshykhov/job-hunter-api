package com.mshykhov.jobhunter.application.job

import com.mshykhov.jobhunter.api.rest.job.dto.JobCheckRequest
import com.mshykhov.jobhunter.api.rest.job.dto.JobCheckResponse
import com.mshykhov.jobhunter.api.rest.job.dto.JobIngestRequest
import com.mshykhov.jobhunter.api.rest.job.dto.PublicJobPageResponse
import com.mshykhov.jobhunter.api.rest.job.dto.PublicJobResponse
import com.mshykhov.jobhunter.application.common.DateTimeParser
import com.mshykhov.jobhunter.infrastructure.config.CacheConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

private val logger = KotlinLogging.logger {}

@Service
class JobService(
    private val jobFacade: JobFacade,
) {
    @Cacheable(CacheConfig.PUBLIC_JOBS_CACHE)
    @Transactional(readOnly = true)
    fun searchPublic(
        page: Int,
        size: Int,
        search: String?,
        source: JobSource?,
        remote: Boolean?,
    ): PublicJobPageResponse {
        val effectiveSize = size.coerceIn(1, MAX_PAGE_SIZE)
        var spec: Specification<JobEntity> = Specification { _, _, _ -> null }

        if (!search.isNullOrBlank()) {
            spec = spec.and(JobSpecifications.search(search))
        }
        if (source != null) {
            spec = spec.and(JobSpecifications.source(source))
        }
        if (remote == true) {
            spec = spec.and(JobSpecifications.remote())
        }

        val pageable =
            PageRequest.of(
                page,
                effectiveSize,
                Sort
                    .by(Sort.Direction.DESC, "publishedAt")
                    .and(Sort.by(Sort.Direction.DESC, "id")),
            )

        val result = jobFacade.findAll(spec, pageable)

        return PublicJobPageResponse(
            content = result.content.map { PublicJobResponse.from(it) },
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }

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

    private fun createNew(request: JobIngestRequest): JobEntity = request.toEntity(parsePublishedAt(request.publishedAt))

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

    fun checkJobs(requests: List<JobCheckRequest>): JobCheckResponse {
        if (requests.isEmpty()) return JobCheckResponse(emptyList(), emptyList(), emptyList())

        val uniqueRequests = requests.associateBy { it.url }.values.toList()
        val urls = uniqueRequests.map { it.url }
        val existingByUrl = jobFacade.findByUrls(urls).associateBy { it.url }

        val newUrls = mutableListOf<String>()
        val updatedUrls = mutableListOf<String>()
        val unchangedUrls = mutableListOf<String>()

        uniqueRequests.forEach { request ->
            val existing = existingByUrl[request.url]
            when {
                existing == null -> newUrls.add(request.url)
                hasDiscoveryChanges(existing, request) -> updatedUrls.add(request.url)
                else -> unchangedUrls.add(request.url)
            }
        }

        logger.info {
            "Check: ${uniqueRequests.size} jobs (${newUrls.size} new, ${updatedUrls.size} updated, ${unchangedUrls.size} unchanged)"
        }

        return JobCheckResponse(newUrls, updatedUrls, unchangedUrls)
    }

    private fun hasDiscoveryChanges(
        entity: JobEntity,
        request: JobCheckRequest,
    ): Boolean {
        if (request.title != null && entity.title != request.title) return true
        if (request.company != null && entity.company != request.company) return true
        if (request.salary != null && entity.salary != request.salary) return true
        if (request.location != null && entity.location != request.location) return true
        val parsedPublishedAt = parsePublishedAt(request.publishedAt)
        if (parsedPublishedAt != null && entity.publishedAt != parsedPublishedAt) return true
        return false
    }

    private fun parsePublishedAt(raw: String?): Instant? {
        val parsed = DateTimeParser.toInstant(raw)
        if (parsed == null && !raw.isNullOrBlank()) {
            logger.warn { "Failed to parse publishedAt: $raw" }
        }
        return parsed
    }

    companion object {
        private const val MAX_PAGE_SIZE = 100
    }
}
