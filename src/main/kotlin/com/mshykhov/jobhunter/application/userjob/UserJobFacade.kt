package com.mshykhov.jobhunter.application.userjob

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
@Transactional(readOnly = true)
class UserJobFacade(
    private val userJobRepository: UserJobRepository,
) {
    fun findByUserId(userId: UUID): List<UserJobEntity> = userJobRepository.findByUserId(userId)

    fun findByUserIdAndStatus(
        userId: UUID,
        status: UserJobStatus,
    ): List<UserJobEntity> = userJobRepository.findByUserIdAndStatus(userId, status)

    fun findByUserIdAndJobId(
        userId: UUID,
        jobId: UUID,
    ): UserJobEntity? = userJobRepository.findByUserIdAndJobId(userId, jobId)

    @Transactional
    fun save(entity: UserJobEntity): UserJobEntity = userJobRepository.save(entity)

    @Transactional
    fun saveAll(entities: List<UserJobEntity>): List<UserJobEntity> = userJobRepository.saveAll(entities)

    @Transactional
    fun deleteByJobIds(jobIds: List<UUID>): Unit = userJobRepository.deleteByJobIdIn(jobIds)
}
