package com.mshykhov.jobhunter.application.job

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class JobFacade(
    private val jobRepository: JobRepository,
) {
    fun findByUrls(urls: List<String>): List<JobEntity> = jobRepository.findByUrlIn(urls)

    fun findUnmatched(): List<JobEntity> = jobRepository.findByMatchedAtIsNull()

    fun saveAll(entities: List<JobEntity>): List<JobEntity> = jobRepository.saveAll(entities)
}
