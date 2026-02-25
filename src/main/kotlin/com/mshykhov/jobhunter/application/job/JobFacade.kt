package com.mshykhov.jobhunter.application.job

import com.mshykhov.jobhunter.application.job.JobEntity
import com.mshykhov.jobhunter.application.job.JobRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class JobFacade(
    private val jobRepository: JobRepository,
) {
    fun findByUrl(url: String): JobEntity? = jobRepository.findByUrl(url)

    fun findByUrls(urls: List<String>): List<JobEntity> = jobRepository.findByUrlIn(urls)

    fun findUnmatched(): List<JobEntity> = jobRepository.findByMatchedAtIsNull()

    fun save(entity: JobEntity): JobEntity = jobRepository.save(entity)

    fun saveAll(entities: List<JobEntity>): List<JobEntity> = jobRepository.saveAll(entities)
}
