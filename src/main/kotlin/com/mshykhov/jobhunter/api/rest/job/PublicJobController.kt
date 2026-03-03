package com.mshykhov.jobhunter.api.rest.job

import com.mshykhov.jobhunter.api.rest.job.dto.PublicJobPageResponse
import com.mshykhov.jobhunter.application.job.JobService
import com.mshykhov.jobhunter.application.job.JobSource
import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.TimeUnit

@RestController
@RequestMapping("/public/jobs")
class PublicJobController(
    private val jobService: JobService,
) {
    @GetMapping
    fun search(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) source: JobSource?,
        @RequestParam(required = false) remote: Boolean?,
    ): ResponseEntity<PublicJobPageResponse> {
        val effectiveSize = size.coerceIn(1, 100)
        val result = jobService.searchPublic(page, effectiveSize, search, source, remote)
        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
            .body(result)
    }
}
