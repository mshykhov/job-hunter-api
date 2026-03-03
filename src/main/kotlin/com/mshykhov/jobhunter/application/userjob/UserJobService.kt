package com.mshykhov.jobhunter.application.userjob

import com.mshykhov.jobhunter.api.rest.exception.custom.NotFoundException
import com.mshykhov.jobhunter.api.rest.job.dto.PaginatedUserJobResponse
import com.mshykhov.jobhunter.api.rest.job.dto.UserJobFilterRequest
import com.mshykhov.jobhunter.api.rest.job.dto.UserJobResponse
import com.mshykhov.jobhunter.application.preference.UserPreferenceFacade
import com.mshykhov.jobhunter.application.user.UserFacade
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserJobService(
    private val userFacade: UserFacade,
    private val userJobFacade: UserJobFacade,
    private val userJobRepository: UserJobRepository,
    private val userPreferenceFacade: UserPreferenceFacade,
) {
    @Transactional(readOnly = true)
    fun search(
        auth0Sub: String,
        filter: UserJobFilterRequest,
    ): PaginatedUserJobResponse {
        val user = userFacade.findByAuth0Sub(auth0Sub)
            ?: return PaginatedUserJobResponse(
                content = emptyList(),
                totalElements = 0,
                hasMore = false,
                size = filter.size,
                statusCounts = emptyMap(),
            )

        val effectiveMinScore = filter.minScore
            ?: userPreferenceFacade.findByUserId(user.id)?.minScore
            ?: 0

        val baseSpec = buildBaseSpec(user.id, effectiveMinScore, filter)
        var fullSpec = if (!filter.statuses.isNullOrEmpty()) {
            baseSpec.and(UserJobSpecifications.statuses(filter.statuses))
        } else {
            baseSpec
        }

        if (filter.cursorCreatedAt != null && filter.cursorId != null) {
            fullSpec = fullSpec.and(
                UserJobSpecifications.beforeCursor(filter.cursorCreatedAt, filter.cursorId),
            )
        }

        val limit = filter.size.coerceIn(1, 100)
        val pageable = PageRequest.of(
            0,
            limit + 1,
            Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")),
        )
        val results = userJobRepository.findAll(fullSpec, pageable).content
        val hasMore = results.size > limit
        val content = if (hasMore) results.take(limit) else results

        val totalElements = userJobRepository.count(
            if (!filter.statuses.isNullOrEmpty()) {
                baseSpec.and(UserJobSpecifications.statuses(filter.statuses))
            } else {
                baseSpec
            },
        )

        val statusCounts = UserJobStatus.entries.associate { status ->
            status to userJobRepository.count(baseSpec.and(UserJobSpecifications.statuses(listOf(status))))
        }

        return PaginatedUserJobResponse(
            content = content.map { UserJobResponse.from(it) },
            totalElements = totalElements,
            hasMore = hasMore,
            size = limit,
            statusCounts = statusCounts,
        )
    }

    private fun buildBaseSpec(
        userId: UUID,
        minScore: Int,
        filter: UserJobFilterRequest,
    ): Specification<UserJobEntity> {
        var spec = UserJobSpecifications.withJobFetch()
            .and(UserJobSpecifications.userId(userId))
            .and(UserJobSpecifications.minScore(minScore))

        if (!filter.sources.isNullOrEmpty()) {
            spec = spec.and(UserJobSpecifications.sources(filter.sources))
        }
        if (filter.publishedAfter != null) {
            spec = spec.and(UserJobSpecifications.publishedAfter(filter.publishedAfter))
        }
        if (filter.matchedAfter != null) {
            spec = spec.and(UserJobSpecifications.matchedAfter(filter.matchedAfter))
        }
        if (filter.updatedAfter != null) {
            spec = spec.and(UserJobSpecifications.updatedAfter(filter.updatedAfter))
        }
        if (filter.remote == true) {
            spec = spec.and(UserJobSpecifications.remote())
        }
        if (!filter.search.isNullOrBlank()) {
            spec = spec.and(UserJobSpecifications.search(filter.search))
        }
        return spec
    }

    @Transactional(readOnly = true)
    fun getDetail(
        auth0Sub: String,
        jobId: UUID,
    ): UserJobEntity {
        val user = findUser(auth0Sub)
        return userJobFacade.findByUserIdAndJobId(user.id, jobId)
            ?: throw NotFoundException("Job $jobId not found for user")
    }

    @Transactional
    fun updateStatus(
        auth0Sub: String,
        jobId: UUID,
        status: UserJobStatus,
    ): UserJobEntity {
        val user = findUser(auth0Sub)
        val userJob =
            userJobFacade.findByUserIdAndJobId(user.id, jobId)
                ?: throw NotFoundException("Job $jobId not found for user")
        userJob.status = status
        return userJobFacade.save(userJob)
    }

    private fun findUser(auth0Sub: String) =
        userFacade.findByAuth0Sub(auth0Sub)
            ?: throw NotFoundException("User not found: $auth0Sub")
}
