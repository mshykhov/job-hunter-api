package com.mshykhov.jobhunter.persistence.facade

import com.mshykhov.jobhunter.persistence.repository.JobRepository
import org.springframework.stereotype.Component

@Component
class JobFacade(
    private val jobRepository: JobRepository,
)
