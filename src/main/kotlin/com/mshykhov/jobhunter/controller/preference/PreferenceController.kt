package com.mshykhov.jobhunter.controller.preference

import com.mshykhov.jobhunter.controller.preference.dto.NormalizePreferenceRequest
import com.mshykhov.jobhunter.controller.preference.dto.PreferenceResponse
import com.mshykhov.jobhunter.controller.preference.dto.SavePreferenceRequest
import com.mshykhov.jobhunter.service.PreferenceNormalizeService
import com.mshykhov.jobhunter.service.PreferenceService
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
@RequestMapping("/preferences")
class PreferenceController(
    private val preferenceService: PreferenceService,
    private val preferenceNormalizeService: PreferenceNormalizeService,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_read:preferences')")
    fun get(
        @AuthenticationPrincipal jwt: Jwt,
    ): PreferenceResponse = preferenceService.get(jwt.subject)

    @PutMapping
    @PreAuthorize("hasAuthority('SCOPE_write:preferences')")
    fun save(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: SavePreferenceRequest,
    ): PreferenceResponse = preferenceService.save(jwt.subject, request)

    @PostMapping("/normalize")
    @PreAuthorize("hasAuthority('SCOPE_write:preferences')")
    fun normalize(
        @Valid @RequestBody request: NormalizePreferenceRequest,
    ): PreferenceResponse = preferenceNormalizeService.normalize(request.rawInput)
}
