package com.mshykhov.jobhunter.api.rest.job

import com.mshykhov.jobhunter.api.rest.job.dto.JobSourceResponse
import com.mshykhov.jobhunter.api.rest.job.dto.PublicJobPageResponse
import com.mshykhov.jobhunter.application.job.JobService
import com.mshykhov.jobhunter.application.job.JobSource
import com.mshykhov.jobhunter.application.job.PublicJobSort
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/public/jobs")
class PublicJobController(
    private val jobService: JobService,
) {
    @GetMapping("/sources")
    fun getSources(): List<JobSourceResponse> = JobSource.entries.map { JobSourceResponse.from(it) }

    @GetMapping
    fun search(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) sources: List<JobSource>?,
        @RequestParam(required = false) remote: Boolean?,
        @RequestParam(required = false) publishedAfter: Instant?,
        @RequestParam(defaultValue = "PUBLISHED") sortBy: PublicJobSort,
    ): PublicJobPageResponse = jobService.searchPublic(page, size, search, sources, remote, publishedAfter, sortBy)
}
