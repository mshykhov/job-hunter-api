package com.mshykhov.jobhunter.application.job

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
@Transactional(readOnly = true)
class JobGroupFacade(private val jobGroupRepository: JobGroupRepository) {
    fun findByGroupKey(groupKey: String): JobGroupEntity? = jobGroupRepository.findByGroupKey(groupKey)

    fun findByGroupKeys(groupKeys: List<String>): List<JobGroupEntity> = jobGroupRepository.findByGroupKeyIn(groupKeys)

    fun findById(id: UUID): JobGroupEntity? = jobGroupRepository.findById(id).orElse(null)

    @Transactional
    fun save(entity: JobGroupEntity): JobGroupEntity = jobGroupRepository.save(entity)

    @Transactional
    fun findOrCreate(
        groupKey: String,
        title: String,
        company: String?,
    ): JobGroupEntity {
        jobGroupRepository.upsert(UUID.randomUUID(), groupKey, title, company)
        return requireNotNull(jobGroupRepository.findByGroupKey(groupKey)) {
            "Job group with key $groupKey should exist after upsert"
        }
    }

    @Transactional
    fun incrementJobCount(id: UUID) = jobGroupRepository.incrementJobCount(id)
}
