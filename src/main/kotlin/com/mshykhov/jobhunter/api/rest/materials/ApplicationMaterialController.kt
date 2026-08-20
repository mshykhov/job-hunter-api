package com.mshykhov.jobhunter.api.rest.materials

import com.mshykhov.jobhunter.api.rest.materials.dto.ApplicationMaterialRequestResponse
import com.mshykhov.jobhunter.api.rest.materials.dto.ApplicationMaterialRevisionResponse
import com.mshykhov.jobhunter.api.rest.materials.dto.CreateMaterialRequest
import com.mshykhov.jobhunter.application.materials.ApplicationMaterialService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/jobs/{jobId}/materials")
class ApplicationMaterialController(private val service: ApplicationMaterialService) {
    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_write:jobs')")
    fun create(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable jobId: UUID,
        @RequestBody request: CreateMaterialRequest,
    ): ApplicationMaterialRequestResponse =
        ApplicationMaterialRequestResponse.from(
            service.ensureReady(
                jwt.subject,
                jobId,
                request.requestedKinds,
                request.mode,
                request.coverLetterPolicy,
                request.regenerate,
            ),
        )

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_read:jobs')")
    fun list(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable jobId: UUID,
    ): List<ApplicationMaterialRequestResponse> =
        service.findForJob(jwt.subject, jobId).map(ApplicationMaterialRequestResponse::from)

    @GetMapping("/revisions")
    @PreAuthorize("hasAuthority('SCOPE_read:jobs')")
    fun revisions(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable jobId: UUID,
    ): List<ApplicationMaterialRevisionResponse> =
        service.findRevisions(jwt.subject, jobId).map(ApplicationMaterialRevisionResponse::from)
}
