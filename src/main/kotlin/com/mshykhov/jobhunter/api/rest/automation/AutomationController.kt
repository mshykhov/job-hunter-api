package com.mshykhov.jobhunter.api.rest.automation

import com.mshykhov.jobhunter.api.rest.automation.dto.AutomationDelegationResponse
import com.mshykhov.jobhunter.api.rest.automation.dto.AutomationStatusResponse
import com.mshykhov.jobhunter.application.automation.AutomationService
import com.mshykhov.jobhunter.infrastructure.automation.AutomationIdentityGuard
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/automation")
class AutomationController(private val service: AutomationService, private val identityGuard: AutomationIdentityGuard) {
    @PutMapping("/delegation")
    fun enable(@AuthenticationPrincipal jwt: Jwt): AutomationDelegationResponse {
        identityGuard.requireOwner(jwt, "write:automation")
        return service.enableDelegation()
    }

    @DeleteMapping("/delegation")
    fun revoke(@AuthenticationPrincipal jwt: Jwt): ResponseEntity<Void> {
        identityGuard.requireOwner(jwt, "write:automation")
        service.revokeDelegation()
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/status")
    fun status(@AuthenticationPrincipal jwt: Jwt): AutomationStatusResponse {
        identityGuard.requireOwner(jwt, "read:automation")
        return service.status()
    }
}
