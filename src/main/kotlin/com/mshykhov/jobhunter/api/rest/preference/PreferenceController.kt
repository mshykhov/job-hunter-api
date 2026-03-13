package com.mshykhov.jobhunter.api.rest.preference

import com.mshykhov.jobhunter.api.rest.preference.dto.AboutRequest
import com.mshykhov.jobhunter.api.rest.preference.dto.AboutResponse
import com.mshykhov.jobhunter.api.rest.preference.dto.MatchingPreferenceRequest
import com.mshykhov.jobhunter.api.rest.preference.dto.MatchingPreferenceResponse
import com.mshykhov.jobhunter.api.rest.preference.dto.NormalizePreferenceResponse
import com.mshykhov.jobhunter.api.rest.preference.dto.PreferenceResponse
import com.mshykhov.jobhunter.api.rest.preference.dto.SearchPreferenceRequest
import com.mshykhov.jobhunter.api.rest.preference.dto.SearchPreferenceResponse
import com.mshykhov.jobhunter.api.rest.preference.dto.TelegramPreferenceRequest
import com.mshykhov.jobhunter.api.rest.preference.dto.TelegramPreferenceResponse
import com.mshykhov.jobhunter.application.preference.PreferenceService
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/preferences")
class PreferenceController(private val preferenceService: PreferenceService) {
    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_read:preferences')")
    fun get(
        @AuthenticationPrincipal jwt: Jwt,
    ): PreferenceResponse = preferenceService.get(jwt.subject)

    @PutMapping("/about")
    @PreAuthorize("hasAuthority('SCOPE_write:preferences')")
    fun saveAbout(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: AboutRequest,
    ): AboutResponse = preferenceService.saveAbout(jwt.subject, request.about)

    @PutMapping("/about/file", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @PreAuthorize("hasAuthority('SCOPE_write:preferences')")
    fun saveAboutFromFile(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam("file") file: MultipartFile,
    ): AboutResponse = preferenceService.saveAboutFromFile(jwt.subject, file)

    @PostMapping("/about/optimize")
    @PreAuthorize("hasAuthority('SCOPE_write:preferences')")
    fun optimizeAbout(
        @AuthenticationPrincipal jwt: Jwt,
    ): AboutResponse = preferenceService.optimizeAbout(jwt.subject)

    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('SCOPE_write:preferences')")
    fun generatePreferences(
        @AuthenticationPrincipal jwt: Jwt,
    ): NormalizePreferenceResponse = preferenceService.generatePreferences(jwt.subject)

    @PutMapping("/search")
    @PreAuthorize("hasAuthority('SCOPE_write:preferences')")
    fun saveSearch(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: SearchPreferenceRequest,
    ): SearchPreferenceResponse = preferenceService.saveSearch(jwt.subject, request)

    @PutMapping("/matching")
    @PreAuthorize("hasAuthority('SCOPE_write:preferences')")
    fun saveMatching(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: MatchingPreferenceRequest,
    ): MatchingPreferenceResponse = preferenceService.saveMatching(jwt.subject, request)

    @PutMapping("/telegram")
    @PreAuthorize("hasAuthority('SCOPE_write:preferences')")
    fun saveTelegram(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: TelegramPreferenceRequest,
    ): TelegramPreferenceResponse = preferenceService.saveTelegram(jwt.subject, request)
}
