package com.mshykhov.jobhunter.application.job

import com.mshykhov.jobhunter.api.rest.job.dto.JobCheckRequest
import com.mshykhov.jobhunter.api.rest.job.dto.JobCheckResponse
import com.mshykhov.jobhunter.api.rest.job.dto.JobIngestRequest
import com.mshykhov.jobhunter.api.rest.job.dto.PublicJobPageResponse
import com.mshykhov.jobhunter.api.rest.job.dto.PublicJobResponse
import com.mshykhov.jobhunter.application.common.DateTimeParser
import com.mshykhov.jobhunter.application.common.PaginationConstants
import com.mshykhov.jobhunter.infrastructure.config.CacheConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

private val logger = KotlinLogging.logger {}

@Service
class JobService(private val jobFacade: JobFacade, private val jobGroupFacade: JobGroupFacade) {
    @Cacheable(CacheConfig.PUBLIC_JOBS_CACHE)
    @Transactional(readOnly = true)
    fun searchPublic(
        page: Int,
        size: Int,
        search: String?,
        sources: List<JobSource>?,
        remote: Boolean?,
        publishedAfter: Instant?,
        sortBy: PublicJobSort = PublicJobSort.PUBLISHED,
    ): PublicJobPageResponse {
        val effectiveSize = size.coerceIn(1, PaginationConstants.MAX_PAGE_SIZE)
        var spec: Specification<JobEntity> = Specification { _, _, _ -> null }

        if (!search.isNullOrBlank()) {
            spec = spec.and(JobSpecifications.search(search))
        }
        if (!sources.isNullOrEmpty()) {
            spec = spec.and(JobSpecifications.sources(sources))
        }
        if (remote == true) {
            spec = spec.and(JobSpecifications.remote())
        }
        if (publishedAfter != null) {
            spec = spec.and(JobSpecifications.publishedAfter(publishedAfter))
        }

        val pageable = PageRequest.of(page.coerceAtLeast(0), effectiveSize, sortBy.sort)
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

        val groupKeys = uniqueRequests.map { JobGroupKeyComputer.compute(it.title, it.company) }.distinct()
        val groupsByKey = jobGroupFacade.findByGroupKeys(groupKeys).associateBy { it.groupKey }.toMutableMap()

        val toSave = mutableListOf<JobEntity>()
        val unchangedEntities = mutableListOf<JobEntity>()

        val groupsToSave = mutableSetOf<JobGroupEntity>()

        uniqueRequests.forEach { request ->
            val existing = existingByUrl[request.url]
            if (existing != null) {
                if (updateExisting(existing, request)) toSave.add(existing) else unchangedEntities.add(existing)
                if (mergeCategory(existing.group, request.category)) groupsToSave.add(existing.group)
            } else {
                val group = findOrCreateGroup(request, groupsByKey)
                mergeCategory(group, request.category)
                groupsToSave.add(group)
                toSave.add(createNew(request, group))
            }
        }

        jobGroupFacade.saveAll(groupsToSave)

        val newCount = toSave.count { it.isNew }
        val updatedCount = toSave.size - newCount
        val sources = (toSave + unchangedEntities).groupingBy { it.source }.eachCount()
        logger.info {
            "Ingest: ${toSave.size + unchangedEntities.size} jobs ($newCount new, $updatedCount updated, ${unchangedEntities.size} unchanged), sources: $sources"
        }

        return jobFacade.saveAll(toSave) + unchangedEntities
    }

    private fun findOrCreateGroup(
        request: JobIngestRequest,
        groupsByKey: MutableMap<String, JobGroupEntity>,
    ): JobGroupEntity {
        val groupKey = JobGroupKeyComputer.compute(request.title, request.company)
        groupsByKey[groupKey]?.let {
            jobGroupFacade.incrementJobCount(it.id)
            return it
        }
        val group = jobGroupFacade.findOrCreate(groupKey, request.title, request.company)
        groupsByKey[groupKey] = group
        return group
    }

    private fun createNew(
        request: JobIngestRequest,
        group: JobGroupEntity,
    ): JobEntity = request.toEntity(parsePublishedAt(request.publishedAt), group)

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

    /** Returns true if category was added, false if already present. */
    private fun mergeCategory(
        group: JobGroupEntity,
        category: Category,
    ): Boolean {
        if (category in group.categories) return false
        group.categories += category
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
}
