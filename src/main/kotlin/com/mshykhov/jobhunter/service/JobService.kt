package com.mshykhov.jobhunter.service

import com.mshykhov.jobhunter.model.dto.JobIngestRequest
import com.mshykhov.jobhunter.model.dto.JobResponse
import com.mshykhov.jobhunter.persistence.facade.JobFacade
import org.springframework.stereotype.Service

@Service
class JobService(
    private val jobFacade: JobFacade,
) {
    fun ingest(requests: List<JobIngestRequest>): List<JobResponse> {
        TODO("implement")
    }
}
