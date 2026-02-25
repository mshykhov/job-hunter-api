package com.mshykhov.jobhunter.controller.job

import com.mshykhov.jobhunter.controller.job.dto.UpdateJobStatusRequest
import com.mshykhov.jobhunter.controller.job.dto.UserJobResponse
import com.mshykhov.jobhunter.persistence.model.UserJobStatus
import com.mshykhov.jobhunter.service.UserJobService
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

    @PatchMapping("/{jobId}/status")
    @PreAuthorize("hasAuthority('SCOPE_write:jobs')")
    fun updateStatus(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable jobId: UUID,
        @Valid @RequestBody request: UpdateJobStatusRequest,
    ): UserJobResponse = userJobService.updateStatus(jwt.subject, jobId, request.status)
}
