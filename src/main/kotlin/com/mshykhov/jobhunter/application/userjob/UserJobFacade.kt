package com.mshykhov.jobhunter.application.userjob

import com.mshykhov.jobhunter.application.common.NotFoundException
import com.mshykhov.jobhunter.application.user.UserEntity
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
@Transactional(readOnly = true)
class UserJobFacade(private val userJobRepository: UserJobRepository, private val userJobGroupFacade: UserJobGroupFacade) {
    fun findByUserIdAndJobId(
        userId: UUID,
        jobId: UUID,
    ): UserJobEntity? = userJobRepository.findByUserIdAndJobId(userId, jobId)

    @Transactional
    fun findOrCreateForGroupMember(
        user: UserEntity,
        jobId: UUID,
    ): UserJobEntity {
        findByUserIdAndJobId(user.id, jobId)?.let { return it }

        val group =
            userJobGroupFacade.findByUserIdAndJobId(user.id, jobId)
                ?: throw NotFoundException("Job not found in your matches")
        val job = group.group.jobs.first { it.id == jobId }

        return userJobRepository.save(UserJobEntity(user = user, job = job))
    }

    fun findByUserIdAndJobIds(
        userId: UUID,
        jobIds: List<UUID>,
    ): List<UserJobEntity> = userJobRepository.findByUserIdAndJobIdIn(userId, jobIds)

    @Transactional
    fun save(entity: UserJobEntity): UserJobEntity = userJobRepository.save(entity)

    @Transactional
    fun saveAll(entities: List<UserJobEntity>): List<UserJobEntity> = userJobRepository.saveAll(entities)

    @Transactional
    fun deleteByJobIds(jobIds: List<UUID>): Unit = userJobRepository.deleteByJobIdIn(jobIds)
}
