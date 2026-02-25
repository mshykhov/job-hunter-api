package com.mshykhov.jobhunter.api.rest.job

import com.mshykhov.jobhunter.api.rest.job.dto.UpdateJobStatusRequest
import com.mshykhov.jobhunter.api.rest.job.dto.UserJobDetailResponse
import com.mshykhov.jobhunter.api.rest.job.dto.UserJobResponse
import com.mshykhov.jobhunter.application.userjob.UserJobService
import com.mshykhov.jobhunter.application.userjob.UserJobStatus
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/jobs")
class UserJobController(
    private val userJobService: UserJobService,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_read:jobs')")
    fun list(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(required = false) status: UserJobStatus?,
    ): List<UserJobResponse> = userJobService.list(jwt.subject, status)

    @GetMapping("/{jobId}")
    @PreAuthorize("hasAuthority('SCOPE_read:jobs')")
    fun getDetail(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable jobId: UUID,
    ): UserJobDetailResponse = userJobService.getDetail(jwt.subject, jobId)

    @PatchMapping("/{jobId}/status")
    @PreAuthorize("hasAuthority('SCOPE_write:jobs')")
    fun updateStatus(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable jobId: UUID,
        @Valid @RequestBody request: UpdateJobStatusRequest,
    ): UserJobResponse = userJobService.updateStatus(jwt.subject, jobId, request.status)
}
