package com.mshykhov.jobhunter.api.rest.settings

import com.mshykhov.jobhunter.api.rest.job.dto.CoverLetterResponse
import com.mshykhov.jobhunter.api.rest.job.dto.RecruiterMessageResponse
import com.mshykhov.jobhunter.api.rest.settings.dto.AiProviderChainResponse
import com.mshykhov.jobhunter.api.rest.settings.dto.AiProvidersResponse
import com.mshykhov.jobhunter.api.rest.settings.dto.AiSettingsResponse
import com.mshykhov.jobhunter.api.rest.settings.dto.OutreachSettingsResponse
import com.mshykhov.jobhunter.api.rest.settings.dto.OutreachTestRequest
import com.mshykhov.jobhunter.api.rest.settings.dto.SaveAiProviderChainRequest
import com.mshykhov.jobhunter.api.rest.settings.dto.SaveAiSettingsRequest
import com.mshykhov.jobhunter.api.rest.settings.dto.SaveOutreachSettingsRequest
import com.mshykhov.jobhunter.application.ai.UserAiProviderService
import com.mshykhov.jobhunter.application.outreach.OutreachService
import com.mshykhov.jobhunter.application.settings.SettingsService
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/settings")
class SettingsController(
    private val settingsService: SettingsService,
    private val userAiProviderService: UserAiProviderService,
    private val outreachService: OutreachService,
) {
    @GetMapping("/ai-providers")
    @PreAuthorize("hasAuthority('SCOPE_read:settings')")
    fun getAiProviders(): AiProvidersResponse = settingsService.getAiProviders()

    @GetMapping("/ai")
    @PreAuthorize("hasAuthority('SCOPE_read:settings')")
    fun getAiSettings(
        @AuthenticationPrincipal jwt: Jwt,
    ): AiSettingsResponse = userAiProviderService.get(jwt.subject)

    @PutMapping("/ai")
    @PreAuthorize("hasAuthority('SCOPE_write:settings')")
    fun saveAiSettings(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: SaveAiSettingsRequest,
    ): AiSettingsResponse = userAiProviderService.save(jwt.subject, request)

    @GetMapping("/ai/providers")
    @PreAuthorize("hasAuthority('SCOPE_read:settings')")
    fun getAiProviderChain(
        @AuthenticationPrincipal jwt: Jwt,
    ): AiProviderChainResponse = userAiProviderService.getChain(jwt.subject)

    @PutMapping("/ai/providers")
    @PreAuthorize("hasAuthority('SCOPE_write:settings')")
    fun saveAiProviderChain(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: SaveAiProviderChainRequest,
    ): AiProviderChainResponse = userAiProviderService.replaceChain(jwt.subject, request)

    @GetMapping("/outreach")
    @PreAuthorize("hasAuthority('SCOPE_read:settings')")
    fun getOutreachSettings(
        @AuthenticationPrincipal jwt: Jwt,
    ): OutreachSettingsResponse = outreachService.getSettings(jwt.subject)

    @PutMapping("/outreach")
    @PreAuthorize("hasAuthority('SCOPE_write:settings')")
    fun saveOutreachSettings(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: SaveOutreachSettingsRequest,
    ): OutreachSettingsResponse = outreachService.saveSettings(jwt.subject, request)

    @PostMapping("/outreach/test/cover-letter")
    @PreAuthorize("hasAuthority('SCOPE_write:settings')")
    fun testCoverLetter(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: OutreachTestRequest,
    ): CoverLetterResponse = outreachService.testCoverLetter(jwt.subject, request.source)

    @PostMapping("/outreach/test/recruiter-message")
    @PreAuthorize("hasAuthority('SCOPE_write:settings')")
    fun testRecruiterMessage(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: OutreachTestRequest,
    ): RecruiterMessageResponse = outreachService.testRecruiterMessage(jwt.subject, request.source)
}
