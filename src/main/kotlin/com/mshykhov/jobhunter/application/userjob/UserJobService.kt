package com.mshykhov.jobhunter.application.userjob

import com.mshykhov.jobhunter.api.rest.job.dto.PaginatedUserJobResponse
import com.mshykhov.jobhunter.api.rest.job.dto.UserJobFilterRequest
import com.mshykhov.jobhunter.api.rest.job.dto.UserJobResponse
import com.mshykhov.jobhunter.application.common.NotFoundException
import com.mshykhov.jobhunter.application.user.UserFacade
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserJobService(
    private val userFacade: UserFacade,
    private val userJobFacade: UserJobFacade,
) {
    @Transactional(readOnly = true)
    fun search(
        auth0Sub: String,
        filter: UserJobFilterRequest,
    ): PaginatedUserJobResponse {
        val user =
            userFacade.findByAuth0Sub(auth0Sub)
                ?: return PaginatedUserJobResponse(
                    content = emptyList(),
                    page = 0,
                    size = filter.size,
                    totalElements = 0,
                    totalPages = 0,
                    statusCounts = emptyMap(),
                )

        val baseSpec = buildBaseSpec(user.id, filter)
        val fullSpec =
            if (!filter.statuses.isNullOrEmpty()) {
                baseSpec.and(UserJobSpecifications.statuses(filter.statuses))
            } else {
                baseSpec
            }

        val effectiveSize = filter.size.coerceIn(1, MAX_PAGE_SIZE)
        val pageable = PageRequest.of(filter.page.coerceAtLeast(0), effectiveSize, filter.sortBy.sort)
        val result = userJobFacade.findAll(fullSpec, pageable)

        val statusCounts =
            UserJobStatus.entries.associate { status ->
                status to userJobFacade.count(baseSpec.and(UserJobSpecifications.statuses(listOf(status))))
            }

        return PaginatedUserJobResponse(
            content = result.content.map { UserJobResponse.from(it) },
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            statusCounts = statusCounts,
        )
    }

    private fun buildBaseSpec(
        userId: UUID,
        filter: UserJobFilterRequest,
    ): Specification<UserJobEntity> {
        var spec =
            UserJobSpecifications
                .withJobFetch()
                .and(UserJobSpecifications.userId(userId))

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
        if (filter.minScore != null) {
            spec = spec.and(UserJobSpecifications.minScore(filter.minScore))
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

    @Transactional
    fun bulkUpdateStatus(
        auth0Sub: String,
        jobIds: List<UUID>,
        status: UserJobStatus,
    ): List<UserJobEntity> {
        val user = findUser(auth0Sub)
        val userJobs = userJobFacade.findByUserIdAndJobIds(user.id, jobIds)
        userJobs.forEach { it.status = status }
        return userJobFacade.saveAll(userJobs)
    }

    private fun findUser(auth0Sub: String) =
        userFacade.findByAuth0Sub(auth0Sub)
            ?: throw NotFoundException("User not found: $auth0Sub")

    companion object {
        private const val MAX_PAGE_SIZE = 100
    }
}
