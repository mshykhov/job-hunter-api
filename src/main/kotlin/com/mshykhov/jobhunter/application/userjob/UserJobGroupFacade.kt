package com.mshykhov.jobhunter.application.userjob

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
@Transactional(readOnly = true)
class UserJobGroupFacade(private val userJobGroupRepository: UserJobGroupRepository) {
    fun findByUserIdAndGroupId(
        userId: UUID,
        groupId: UUID,
    ): UserJobGroupEntity? = userJobGroupRepository.findByUserIdAndGroupId(userId, groupId)

    fun findByUserIdAndJobId(
        userId: UUID,
        jobId: UUID,
    ): UserJobGroupEntity? = userJobGroupRepository.findByUserIdAndJobId(userId, jobId)

    fun findByGroupId(groupId: UUID): List<UserJobGroupEntity> = userJobGroupRepository.findByGroupId(groupId)

    fun findByUserIdAndGroupIds(
        userId: UUID,
        groupIds: List<UUID>,
    ): List<UserJobGroupEntity> = userJobGroupRepository.findByUserIdAndGroupIdIn(userId, groupIds)

    fun findAll(
        spec: Specification<UserJobGroupEntity>,
        pageable: Pageable,
    ): Page<UserJobGroupEntity> = userJobGroupRepository.findAll(spec, pageable)

    fun count(spec: Specification<UserJobGroupEntity>): Long = userJobGroupRepository.count(spec)

    @Transactional
    fun save(entity: UserJobGroupEntity): UserJobGroupEntity = userJobGroupRepository.save(entity)

    @Transactional
    fun saveAll(entities: List<UserJobGroupEntity>): List<UserJobGroupEntity> = userJobGroupRepository.saveAll(entities)
}
