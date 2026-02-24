package com.mshykhov.jobhunter.service

import com.mshykhov.jobhunter.persistence.facade.JobFacade
import org.springframework.stereotype.Service

@Service
class JobService(
    private val jobFacade: JobFacade,
)
