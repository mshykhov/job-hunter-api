package com.mshykhov.jobhunter.api.rest.job

import com.mshykhov.jobhunter.api.rest.job.dto.PaginatedUserJobResponse
import com.mshykhov.jobhunter.api.rest.job.dto.RematchResponse
import com.mshykhov.jobhunter.api.rest.job.dto.UpdateJobStatusRequest
import com.mshykhov.jobhunter.api.rest.job.dto.UserJobDetailResponse
import com.mshykhov.jobhunter.api.rest.job.dto.UserJobFilterRequest
import com.mshykhov.jobhunter.api.rest.job.dto.UserJobResponse
import com.mshykhov.jobhunter.application.matching.JobMatchingService
import com.mshykhov.jobhunter.application.userjob.UserJobService
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/jobs")
class UserJobController(
    private val userJobService: UserJobService,
    private val jobMatchingService: JobMatchingService,
) {
    @PostMapping("/search")
    @PreAuthorize("hasAuthority('SCOPE_read:jobs')")
    fun search(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody filter: UserJobFilterRequest,
    ): PaginatedUserJobResponse = userJobService.search(jwt.subject, filter)

    @GetMapping("/{jobId}")
    @PreAuthorize("hasAuthority('SCOPE_read:jobs')")
    fun getDetail(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable jobId: UUID,
    ): UserJobDetailResponse = UserJobDetailResponse.from(userJobService.getDetail(jwt.subject, jobId))

    @PatchMapping("/{jobId}/status")
    @PreAuthorize("hasAuthority('SCOPE_write:jobs')")
    fun updateStatus(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable jobId: UUID,
        @Valid @RequestBody request: UpdateJobStatusRequest,
    ): UserJobResponse = UserJobResponse.from(userJobService.updateStatus(jwt.subject, jobId, request.status))

    @PostMapping("/rematch")
    @PreAuthorize("hasAuthority('SCOPE_write:jobs')")
    fun rematch(
        @RequestParam(required = false) since: Instant?,
    ): RematchResponse {
        val count = jobMatchingService.rematch(since)
        return RematchResponse(jobsQueued = count)
    }
}
