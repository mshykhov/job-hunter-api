package com.mshykhov.jobhunter.application.userjob

import com.mshykhov.jobhunter.api.rest.job.dto.UserJobResponse
import com.mshykhov.jobhunter.application.common.NotFoundException
import com.mshykhov.jobhunter.application.user.UserFacade
import com.mshykhov.jobhunter.application.userjob.UserJobFacade
import com.mshykhov.jobhunter.application.userjob.UserJobStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserJobService(
    private val userFacade: UserFacade,
    private val userJobFacade: UserJobFacade,
) {
    fun list(
        auth0Sub: String,
        status: UserJobStatus?,
    ): List<UserJobResponse> {
        val user = findUser(auth0Sub)
        val userJobs =
            if (status != null) {
                userJobFacade.findByUserIdAndStatus(user.id, status)
            } else {
                userJobFacade.findByUserId(user.id)
            }
        return userJobs.map { UserJobResponse.from(it) }
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
