package com.mshykhov.jobhunter.api.rest.settings

import com.mshykhov.jobhunter.api.rest.settings.dto.AiProvidersResponse
import com.mshykhov.jobhunter.application.settings.SettingsService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/settings")
class SettingsController(
    private val settingsService: SettingsService,
) {
    @GetMapping("/ai-providers")
    @PreAuthorize("hasAuthority('SCOPE_read:settings')")
    fun getAiProviders(): AiProvidersResponse = settingsService.getAiProviders()
}
