package com.mshykhov.jobhunter.application.userjob

import com.mshykhov.jobhunter.api.rest.job.dto.GroupJobResponse
import com.mshykhov.jobhunter.api.rest.job.dto.JobGroupDetailResponse
import com.mshykhov.jobhunter.api.rest.job.dto.PaginatedUserJobGroupResponse
import com.mshykhov.jobhunter.api.rest.job.dto.UserJobGroupFilterRequest
import com.mshykhov.jobhunter.api.rest.job.dto.UserJobGroupResponse
import com.mshykhov.jobhunter.application.common.NotFoundException
import com.mshykhov.jobhunter.application.common.PaginationConstants
import com.mshykhov.jobhunter.application.job.JobFacade
import com.mshykhov.jobhunter.application.user.UserFacade
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserJobGroupService(
    private val userFacade: UserFacade,
    private val userJobGroupFacade: UserJobGroupFacade,
    private val userJobFacade: UserJobFacade,
    private val jobFacade: JobFacade,
) {
    @Transactional(readOnly = true)
    fun search(
        auth0Sub: String,
        filter: UserJobGroupFilterRequest,
    ): PaginatedUserJobGroupResponse {
        val user =
            userFacade.findByAuth0Sub(auth0Sub)
                ?: return PaginatedUserJobGroupResponse(
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
                baseSpec.and(UserJobGroupSpecifications.statuses(filter.statuses))
            } else {
                baseSpec
            }

        val effectiveSize = filter.size.coerceIn(1, PaginationConstants.MAX_PAGE_SIZE)
        val pageable = PageRequest.of(filter.page.coerceAtLeast(0), effectiveSize, filter.sortBy.sort)
        val result = userJobGroupFacade.findAll(fullSpec, pageable)

        val statusCounts =
            UserJobStatus.entries.associate { status ->
                status to userJobGroupFacade.count(baseSpec.and(UserJobGroupSpecifications.statuses(listOf(status))))
            }

        return PaginatedUserJobGroupResponse(
            content = result.content.map { UserJobGroupResponse.from(it) },
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            statusCounts = statusCounts,
        )
    }

    @Transactional(readOnly = true)
    fun getGroupDetail(
        auth0Sub: String,
        groupId: UUID,
    ): JobGroupDetailResponse {
        val user =
            userFacade.findByAuth0Sub(auth0Sub)
                ?: throw NotFoundException("User not found: $auth0Sub")

        val userJobGroup =
            userJobGroupFacade.findByUserIdAndGroupId(user.id, groupId)
                ?: throw NotFoundException("Group $groupId not found for user")

        val jobs = jobFacade.findByGroupId(groupId)
        val userJobsByJobId = userJobFacade.findByUserIdAndJobIds(user.id, jobs.map { it.id }).associateBy { it.job.id }

        val jobResponses =
            jobs.map { job ->
                GroupJobResponse.from(job, userJobsByJobId[job.id])
            }

        return JobGroupDetailResponse.from(userJobGroup, jobResponses)
    }

    @Transactional
    fun updateGroupStatus(
        auth0Sub: String,
        groupId: UUID,
        status: UserJobStatus,
    ): UserJobGroupResponse {
        val user =
            userFacade.findByAuth0Sub(auth0Sub)
                ?: throw NotFoundException("User not found: $auth0Sub")

        val userJobGroup =
            userJobGroupFacade.findByUserIdAndGroupId(user.id, groupId)
                ?: throw NotFoundException("Group $groupId not found for user")

        userJobGroup.status = status
        val saved = userJobGroupFacade.save(userJobGroup)
        return UserJobGroupResponse.from(saved)
    }

    @Transactional
    fun bulkUpdateGroupStatus(
        auth0Sub: String,
        groupIds: List<UUID>,
        status: UserJobStatus,
    ): List<UserJobGroupResponse> {
        val user =
            userFacade.findByAuth0Sub(auth0Sub)
                ?: throw NotFoundException("User not found: $auth0Sub")

        val userJobGroups = userJobGroupFacade.findByUserIdAndGroupIds(user.id, groupIds)
        val foundGroupIds = userJobGroups.map { it.group.id }.toSet()
        val missingGroupIds = groupIds.filter { it !in foundGroupIds }
        if (missingGroupIds.isNotEmpty()) {
            throw NotFoundException("Groups not found for user: $missingGroupIds")
        }
        userJobGroups.forEach { it.status = status }
        val saved = userJobGroupFacade.saveAll(userJobGroups)
        return saved.map { UserJobGroupResponse.from(it) }
    }

    private fun buildBaseSpec(
        userId: UUID,
        filter: UserJobGroupFilterRequest,
    ): Specification<UserJobGroupEntity> {
        var spec =
            UserJobGroupSpecifications
                .withGroupFetch()
                .and(UserJobGroupSpecifications.userId(userId))

        if (filter.matchedAfter != null) {
            spec = spec.and(UserJobGroupSpecifications.matchedAfter(filter.matchedAfter))
        }
        if (filter.remote == true) {
            spec = spec.and(UserJobGroupSpecifications.remote())
        }
        if (!filter.sources.isNullOrEmpty()) {
            spec = spec.and(UserJobGroupSpecifications.sources(filter.sources))
        }
        if (!filter.search.isNullOrBlank()) {
            spec = spec.and(UserJobGroupSpecifications.search(filter.search))
        }
        if (filter.minScore != null) {
            spec = spec.and(UserJobGroupSpecifications.minScore(filter.minScore))
        }
        return spec
    }
}
