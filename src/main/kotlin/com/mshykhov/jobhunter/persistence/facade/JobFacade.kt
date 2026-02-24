package com.mshykhov.jobhunter.persistence.facade

import com.mshykhov.jobhunter.persistence.model.JobEntity
import com.mshykhov.jobhunter.persistence.repository.JobRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class JobFacade(
    private val jobRepository: JobRepository,
) {
    fun findByUrl(url: String): JobEntity? = jobRepository.findByUrl(url)

    fun save(entity: JobEntity): JobEntity = jobRepository.save(entity)
}
