package com.mshykhov.jobhunter.application.job

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Component
@Transactional(readOnly = true)
class JobFacade(private val jobRepository: JobRepository) {
    fun findAll(
        spec: Specification<JobEntity>,
        pageable: Pageable,
    ): Page<JobEntity> = jobRepository.findAll(spec, pageable)

    fun findByUrls(urls: List<String>): List<JobEntity> = jobRepository.findByUrlIn(urls)

    fun findByGroupId(groupId: UUID): List<JobEntity> = jobRepository.findByGroupId(groupId)

    fun findByGroupIds(
        groupIds: List<UUID>,
        limit: Int,
        maxAttempts: Int,
    ): List<JobEntity> =
        jobRepository.findByGroupIdInAndMatchedAtIsNullAndMatchAttemptsLessThan(
            groupIds,
            maxAttempts,
            PageRequest.of(0, limit, Sort.by(Sort.Direction.ASC, "createdAt")),
        )

    fun findUnmatched(
        limit: Int,
        maxAttempts: Int,
    ): List<JobEntity> =
        jobRepository.findByMatchedAtIsNullAndMatchAttemptsLessThan(
            maxAttempts,
            PageRequest.of(0, limit, Sort.by(Sort.Direction.ASC, "createdAt")),
        )

    fun findAllMatched(): List<JobEntity> = jobRepository.findByMatchedAtIsNotNull()

    fun findMatchedSince(since: Instant): List<JobEntity> = jobRepository.findByMatchedAtGreaterThanEqual(since)

    fun findTopBySourceOrderByCreatedAtDesc(source: JobSource): JobEntity? = jobRepository.findTopBySourceOrderByCreatedAtDesc(source)

    @Transactional
    fun saveAll(entities: List<JobEntity>): List<JobEntity> = jobRepository.saveAll(entities)

    @Transactional
    fun updateMatchedAt(
        ids: List<UUID>,
        matchedAt: Instant?,
    ) = jobRepository.updateMatchedAt(ids, matchedAt)

    @Transactional
    fun updateRemote(
        id: UUID,
        remote: Boolean,
    ) = jobRepository.updateRemote(id, remote)

    @Transactional
    fun incrementMatchAttempts(ids: List<UUID>) = jobRepository.incrementMatchAttempts(ids)
}
