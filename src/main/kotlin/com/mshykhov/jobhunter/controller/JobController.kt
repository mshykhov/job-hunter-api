package com.mshykhov.jobhunter.controller

import com.mshykhov.jobhunter.service.JobService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/jobs")
class JobController(
    private val jobService: JobService,
) {
    @PostMapping("/ingest")
    @PreAuthorize("hasAuthority('SCOPE_write:jobs')")
    fun ingest() {
        // TODO: implement
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_read:jobs')")
    fun list() {
        // TODO: implement
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('SCOPE_manage:jobs')")
    fun updateStatus() {
        // TODO: implement
    }
}
