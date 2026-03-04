package com.mshykhov.jobhunter.api.rest.settings

import com.mshykhov.jobhunter.api.rest.settings.dto.AiProvidersResponse
import com.mshykhov.jobhunter.api.rest.settings.dto.AiSettingsResponse
import com.mshykhov.jobhunter.api.rest.settings.dto.SaveAiSettingsRequest
import com.mshykhov.jobhunter.application.ai.UserAiSettingsService
import com.mshykhov.jobhunter.application.settings.SettingsService
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/settings")
class SettingsController(
    private val settingsService: SettingsService,
    private val userAiSettingsService: UserAiSettingsService,
) {
    @GetMapping("/ai-providers")
    @PreAuthorize("hasAuthority('SCOPE_read:settings')")
    fun getAiProviders(): AiProvidersResponse = settingsService.getAiProviders()

    @GetMapping("/ai")
    @PreAuthorize("hasAuthority('SCOPE_read:settings')")
    fun getAiSettings(
        @AuthenticationPrincipal jwt: Jwt,
    ): AiSettingsResponse = userAiSettingsService.get(jwt.subject)

    @PutMapping("/ai")
    @PreAuthorize("hasAuthority('SCOPE_write:settings')")
    fun saveAiSettings(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: SaveAiSettingsRequest,
    ): AiSettingsResponse = userAiSettingsService.save(jwt.subject, request)
}
