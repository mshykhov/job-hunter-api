package com.mshykhov.jobhunter.api.rest.materials

import com.mshykhov.jobhunter.api.rest.materials.dto.CandidateProfileSummaryResponse
import com.mshykhov.jobhunter.application.materials.CandidateProfileService
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/materials/profiles")
class MaterialProfileController(private val service: CandidateProfileService) {
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @PreAuthorize("hasAuthority('SCOPE_write:jobs')")
    fun importProfile(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestPart manifest: MultipartFile,
        @RequestPart candidateProfile: MultipartFile,
        @RequestPart factCatalog: MultipartFile,
        @RequestPart writingStyle: MultipartFile,
        @RequestPart baseCvDocx: MultipartFile,
        @RequestPart baseCvPdf: MultipartFile,
    ): CandidateProfileSummaryResponse =
        CandidateProfileSummaryResponse.from(
            service.importProfile(
                jwt.subject,
                manifest.bytes,
                candidateProfile.bytes,
                factCatalog.bytes,
                writingStyle.bytes,
                baseCvDocx.bytes,
                baseCvPdf.bytes,
            ),
        )

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_read:jobs')")
    fun listProfiles(@AuthenticationPrincipal jwt: Jwt): List<CandidateProfileSummaryResponse> =
        service.listProfiles(jwt.subject).map(CandidateProfileSummaryResponse::from)
}
