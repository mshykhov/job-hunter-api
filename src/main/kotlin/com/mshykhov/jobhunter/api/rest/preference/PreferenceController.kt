package com.mshykhov.jobhunter.api.rest.preference

import com.mshykhov.jobhunter.api.rest.preference.dto.NormalizePreferenceRequest
import com.mshykhov.jobhunter.api.rest.preference.dto.PreferenceResponse
import com.mshykhov.jobhunter.api.rest.preference.dto.SavePreferenceRequest
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
class PreferenceController(
    private val preferenceService: PreferenceService,
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
    ): PreferenceResponse = preferenceService.normalize(request.rawInput)

    @PostMapping("/normalize/file", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @PreAuthorize("hasAuthority('SCOPE_write:preferences')")
    fun normalizeFile(
        @RequestParam("file") file: MultipartFile,
    ): PreferenceResponse = preferenceService.normalizeFile(file)
}
