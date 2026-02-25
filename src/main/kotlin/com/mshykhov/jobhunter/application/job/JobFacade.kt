package com.mshykhov.jobhunter.application.job

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
@Transactional
class JobFacade(
    private val jobRepository: JobRepository,
) {
    fun findByUrls(urls: List<String>): List<JobEntity> = jobRepository.findByUrlIn(urls)

    fun findUnmatched(): List<JobEntity> = jobRepository.findByMatchedAtIsNull()

    fun findAllMatched(): List<JobEntity> = jobRepository.findByMatchedAtIsNotNull()

    fun findMatchedSince(since: Instant): List<JobEntity> = jobRepository.findByMatchedAtGreaterThanEqual(since)

    fun saveAll(entities: List<JobEntity>): List<JobEntity> = jobRepository.saveAll(entities)
}
