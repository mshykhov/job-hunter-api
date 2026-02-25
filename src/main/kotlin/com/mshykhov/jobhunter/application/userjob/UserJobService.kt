package com.mshykhov.jobhunter.application.userjob

import com.mshykhov.jobhunter.api.rest.exception.custom.NotFoundException
import com.mshykhov.jobhunter.api.rest.job.dto.UserJobDetailResponse
import com.mshykhov.jobhunter.api.rest.job.dto.UserJobResponse
import com.mshykhov.jobhunter.application.preference.UserPreferenceFacade
import com.mshykhov.jobhunter.application.user.UserFacade
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserJobService(
    private val userFacade: UserFacade,
    private val userJobFacade: UserJobFacade,
    private val userPreferenceFacade: UserPreferenceFacade,
) {
    @Transactional(readOnly = true)
    fun list(
        auth0Sub: String,
        status: UserJobStatus?,
        minScore: Int?,
    ): List<UserJobResponse> {
        val user = userFacade.findByAuth0Sub(auth0Sub) ?: return emptyList()
        val effectiveMinScore = minScore ?: userPreferenceFacade.findByUserId(user.id)?.minScore ?: 0
        val userJobs =
            if (status != null) {
                userJobFacade.findByUserIdAndStatus(user.id, status)
            } else {
                userJobFacade.findByUserId(user.id)
            }
        return userJobs
            .filter { it.aiRelevanceScore >= effectiveMinScore }
            .map { UserJobResponse.from(it) }
    }

    @Transactional(readOnly = true)
    fun getDetail(
        auth0Sub: String,
        jobId: UUID,
    ): UserJobDetailResponse {
        val user = findUser(auth0Sub)
        val userJob =
            userJobFacade.findByUserIdAndJobId(user.id, jobId)
                ?: throw NotFoundException("Job $jobId not found for user")
        return UserJobDetailResponse.from(userJob)
    }

    @Transactional
    fun updateStatus(
        auth0Sub: String,
        jobId: UUID,
        status: UserJobStatus,
    ): UserJobResponse {
        val user = findUser(auth0Sub)
        val userJob =
            userJobFacade.findByUserIdAndJobId(user.id, jobId)
                ?: throw NotFoundException("Job $jobId not found for user")
        userJob.status = status
        return UserJobResponse.from(userJobFacade.save(userJob))
    }

    private fun findUser(auth0Sub: String) =
        userFacade.findByAuth0Sub(auth0Sub)
            ?: throw NotFoundException("User not found: $auth0Sub")
}
