package com.mshykhov.jobhunter.api.rest.automation

import com.mshykhov.jobhunter.api.rest.automation.dto.AutomationHeartbeatRequest
import com.mshykhov.jobhunter.api.rest.automation.dto.AutomationHeartbeatResponse
import com.mshykhov.jobhunter.api.rest.automation.dto.AutomationSessionResponse
import com.mshykhov.jobhunter.application.automation.AutomationService
import com.mshykhov.jobhunter.infrastructure.automation.AutomationIdentityGuard
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/automation/runner")
class AutomationRunnerController(private val service: AutomationService, private val identityGuard: AutomationIdentityGuard) {
    @PostMapping("/session")
    fun startSession(@AuthenticationPrincipal jwt: Jwt): AutomationSessionResponse {
        identityGuard.requireRunner(jwt)
        return service.startSession()
    }

    @PutMapping("/heartbeat")
    fun heartbeat(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: AutomationHeartbeatRequest,
    ): AutomationHeartbeatResponse {
        identityGuard.requireRunner(jwt)
        return service.recordHeartbeat(request)
    }
}
