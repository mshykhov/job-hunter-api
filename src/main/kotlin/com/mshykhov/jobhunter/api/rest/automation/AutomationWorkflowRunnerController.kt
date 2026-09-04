package com.mshykhov.jobhunter.api.rest.automation

import com.mshykhov.jobhunter.api.rest.automation.dto.workflow.AutomationCheckpointRequest
import com.mshykhov.jobhunter.api.rest.automation.dto.workflow.AutomationFailureRequest
import com.mshykhov.jobhunter.api.rest.automation.dto.workflow.AutomationLeaseRequest
import com.mshykhov.jobhunter.api.rest.automation.dto.workflow.AutomationWorkClaimRequest
import com.mshykhov.jobhunter.api.rest.automation.dto.workflow.AutomationWorkClaimResponse
import com.mshykhov.jobhunter.api.rest.automation.dto.workflow.AutomationWorkProgressResponse
import com.mshykhov.jobhunter.application.automation.workflow.AutomationWorkflowService
import com.mshykhov.jobhunter.infrastructure.automation.AutomationIdentityGuard
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/automation/runner/work-items")
class AutomationWorkflowRunnerController(private val service: AutomationWorkflowService, private val identityGuard: AutomationIdentityGuard) {
    @PostMapping("/claims")
    fun claim(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: AutomationWorkClaimRequest,
    ): ResponseEntity<AutomationWorkClaimResponse> {
        identityGuard.requireRunner(jwt)
        val claim = service.claim(request.workerId, request.generation) ?: return ResponseEntity.noContent().build()
        return ResponseEntity.ok(AutomationWorkClaimResponse.from(claim))
    }

    @PostMapping("/{workItemId}/heartbeat")
    fun heartbeat(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable workItemId: UUID,
        @Valid @RequestBody request: AutomationLeaseRequest,
    ): AutomationWorkProgressResponse {
        identityGuard.requireRunner(jwt)
        return AutomationWorkProgressResponse.from(service.heartbeat(workItemId, request.toCommand()))
    }

    @PostMapping("/{workItemId}/checkpoints")
    fun checkpoint(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable workItemId: UUID,
        @Valid @RequestBody request: AutomationCheckpointRequest,
    ): AutomationWorkProgressResponse {
        identityGuard.requireRunner(jwt)
        return AutomationWorkProgressResponse.from(service.checkpoint(workItemId, request.toCommand()))
    }

    @PostMapping("/{workItemId}/complete")
    fun complete(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable workItemId: UUID,
        @Valid @RequestBody request: AutomationLeaseRequest,
    ): AutomationWorkProgressResponse {
        identityGuard.requireRunner(jwt)
        return AutomationWorkProgressResponse.from(service.complete(workItemId, request.toCommand()))
    }

    @PostMapping("/{workItemId}/fail")
    fun fail(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable workItemId: UUID,
        @Valid @RequestBody request: AutomationFailureRequest,
    ): AutomationWorkProgressResponse {
        identityGuard.requireRunner(jwt)
        return AutomationWorkProgressResponse.from(service.fail(workItemId, request.toCommand()))
    }
}
