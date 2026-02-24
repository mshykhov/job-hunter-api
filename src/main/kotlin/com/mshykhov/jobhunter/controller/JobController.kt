package com.mshykhov.jobhunter.controller

import com.mshykhov.jobhunter.model.dto.JobIngestRequest
import com.mshykhov.jobhunter.model.dto.JobResponse
import com.mshykhov.jobhunter.service.JobService
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/jobs")
class JobController(
    private val jobService: JobService,
) {
    @PostMapping("/ingest")
    @PreAuthorize("hasAuthority('SCOPE_write:jobs')")
    fun ingest(
        @Valid @RequestBody request: List<JobIngestRequest>,
    ): List<JobResponse> = jobService.ingest(request)
}
