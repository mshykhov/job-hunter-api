package com.mshykhov.jobhunter.application.userjob

import com.mshykhov.jobhunter.api.rest.exception.custom.NotFoundException
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
    ): List<UserJobEntity> {
        val user = userFacade.findByAuth0Sub(auth0Sub) ?: return emptyList()
        val effectiveMinScore = minScore ?: userPreferenceFacade.findByUserId(user.id)?.minScore ?: 0
        val userJobs =
            if (status != null) {
                userJobFacade.findByUserIdAndStatus(user.id, status)
            } else {
                userJobFacade.findByUserId(user.id)
            }
        return userJobs.filter { it.aiRelevanceScore >= effectiveMinScore }
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
