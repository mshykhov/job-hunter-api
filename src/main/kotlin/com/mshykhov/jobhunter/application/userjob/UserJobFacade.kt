package com.mshykhov.jobhunter.application.userjob

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
@Transactional(readOnly = true)
class UserJobFacade(
    private val userJobRepository: UserJobRepository,
) {
    fun findUserIdsByJobId(jobId: UUID): Set<UUID> = userJobRepository.findUserIdsByJobId(jobId)

    fun findByJobId(jobId: UUID): List<UserJobEntity> = userJobRepository.findByJobId(jobId)

    fun findByUserIdAndJobId(
        userId: UUID,
        jobId: UUID,
    ): UserJobEntity? = userJobRepository.findByUserIdAndJobId(userId, jobId)

    fun findByUserIdAndJobIds(
        userId: UUID,
        jobIds: List<UUID>,
    ): List<UserJobEntity> = userJobRepository.findByUserIdAndJobIdIn(userId, jobIds)

    fun findAll(
        spec: Specification<UserJobEntity>,
        pageable: Pageable,
    ): Page<UserJobEntity> = userJobRepository.findAll(spec, pageable)

    fun count(spec: Specification<UserJobEntity>): Long = userJobRepository.count(spec)

    @Transactional
    fun save(entity: UserJobEntity): UserJobEntity = userJobRepository.save(entity)

    @Transactional
    fun saveAll(entities: List<UserJobEntity>): List<UserJobEntity> = userJobRepository.saveAll(entities)

    @Transactional
    fun deleteByJobIds(jobIds: List<UUID>): Unit = userJobRepository.deleteByJobIdIn(jobIds)
}
