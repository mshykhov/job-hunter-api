package com.mshykhov.jobhunter.api.rest.automation

import com.mshykhov.jobhunter.api.rest.automation.dto.workflow.AutomationWorkflowRunResponse
import com.mshykhov.jobhunter.api.rest.automation.dto.workflow.AutomationWorkflowRunSummaryResponse
import com.mshykhov.jobhunter.api.rest.automation.dto.workflow.CreateAutomationWorkflowRunRequest
import com.mshykhov.jobhunter.application.automation.workflow.AutomationWorkflowService
import com.mshykhov.jobhunter.infrastructure.automation.AutomationIdentityGuard
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/automation/workflows/runs")
class AutomationWorkflowController(private val service: AutomationWorkflowService, private val identityGuard: AutomationIdentityGuard) {
    @PostMapping
    fun create(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: CreateAutomationWorkflowRunRequest,
    ): AutomationWorkflowRunResponse {
        identityGuard.requireOwner(jwt, "write:automation")
        return AutomationWorkflowRunResponse.from(service.createRun(request.idempotencyKey))
    }

    @GetMapping
    fun list(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(defaultValue = "20") limit: Int,
    ): List<AutomationWorkflowRunSummaryResponse> {
        identityGuard.requireOwner(jwt, "read:automation")
        return service.listRuns(limit).map(AutomationWorkflowRunSummaryResponse::from)
    }

    @GetMapping("/{runId}")
    fun get(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable runId: UUID,
    ): AutomationWorkflowRunResponse {
        identityGuard.requireOwner(jwt, "read:automation")
        return AutomationWorkflowRunResponse.from(service.getRun(runId))
    }

    @PostMapping("/{runId}/pause")
    fun pause(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable runId: UUID,
    ): AutomationWorkflowRunResponse {
        identityGuard.requireOwner(jwt, "write:automation")
        return AutomationWorkflowRunResponse.from(service.pause(runId))
    }

    @PostMapping("/{runId}/resume")
    fun resume(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable runId: UUID,
    ): AutomationWorkflowRunResponse {
        identityGuard.requireOwner(jwt, "write:automation")
        return AutomationWorkflowRunResponse.from(service.resume(runId))
    }

    @PostMapping("/{runId}/stop")
    fun stop(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable runId: UUID,
    ): AutomationWorkflowRunResponse {
        identityGuard.requireOwner(jwt, "write:automation")
        return AutomationWorkflowRunResponse.from(service.stop(runId))
    }
}
