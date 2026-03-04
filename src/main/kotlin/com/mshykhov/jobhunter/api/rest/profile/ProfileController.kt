package com.mshykhov.jobhunter.api.rest.profile

import com.mshykhov.jobhunter.api.rest.profile.dto.ProfileResponse
import com.mshykhov.jobhunter.application.profile.ProfileService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/profile")
class ProfileController(
    private val profileService: ProfileService,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_read:profile')")
    fun get(
        @AuthenticationPrincipal jwt: Jwt,
    ): ProfileResponse = profileService.get(jwt.subject)
}
