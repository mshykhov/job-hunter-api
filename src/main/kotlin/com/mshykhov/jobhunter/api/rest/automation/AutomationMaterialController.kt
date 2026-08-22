package com.mshykhov.jobhunter.api.rest.automation

import com.mshykhov.jobhunter.api.rest.automation.dto.AutomationMaterialClaimRequest
import com.mshykhov.jobhunter.api.rest.automation.dto.AutomationMaterialClaimResponse
import com.mshykhov.jobhunter.api.rest.automation.dto.AutomationMaterialCompletionMetadata
import com.mshykhov.jobhunter.api.rest.automation.dto.AutomationMaterialCompletionResponse
import com.mshykhov.jobhunter.api.rest.automation.dto.AutomationMaterialFailureRequest
import com.mshykhov.jobhunter.api.rest.automation.dto.AutomationMaterialHeartbeatRequest
import com.mshykhov.jobhunter.api.rest.automation.dto.AutomationMaterialHeartbeatResponse
import com.mshykhov.jobhunter.api.rest.materials.dto.CandidateProfileSummaryResponse
import com.mshykhov.jobhunter.application.common.ValidationException
import com.mshykhov.jobhunter.application.materials.CandidateProfileService
import com.mshykhov.jobhunter.application.materials.MaterialArtifactUpload
import com.mshykhov.jobhunter.application.materials.MaterialCompletion
import com.mshykhov.jobhunter.application.materials.MaterialKind
import com.mshykhov.jobhunter.application.materials.MaterialLeaseService
import com.mshykhov.jobhunter.infrastructure.automation.AutomationIdentityGuard
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/automation/materials")
class AutomationMaterialController(
    private val service: MaterialLeaseService,
    private val profileService: CandidateProfileService,
    private val identityGuard: AutomationIdentityGuard,
) {
    @PostMapping("/profile", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun importProfile(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestPart manifest: MultipartFile,
        @RequestPart candidateProfile: MultipartFile,
        @RequestPart factCatalog: MultipartFile,
        @RequestPart writingStyle: MultipartFile,
        @RequestPart baseCvDocx: MultipartFile,
        @RequestPart baseCvPdf: MultipartFile,
    ): CandidateProfileSummaryResponse {
        identityGuard.requireRunner(jwt)
        return CandidateProfileSummaryResponse.from(
            profileService.importProfile(
                identityGuard.ownerSubject(),
                manifest.bytes,
                candidateProfile.bytes,
                factCatalog.bytes,
                writingStyle.bytes,
                baseCvDocx.bytes,
                baseCvPdf.bytes,
            ),
        )
    }

    @PostMapping("/claims")
    fun claim(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: AutomationMaterialClaimRequest,
    ): ResponseEntity<AutomationMaterialClaimResponse> {
        identityGuard.requireRunner(jwt)
        val claim = service.claim(request.workerId) ?: return ResponseEntity.noContent().build()
        return ResponseEntity.ok(AutomationMaterialClaimResponse.from(claim))
    }

    @PostMapping("/{requestId}/heartbeat")
    fun heartbeat(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable requestId: UUID,
        @RequestBody request: AutomationMaterialHeartbeatRequest,
    ): AutomationMaterialHeartbeatResponse {
        identityGuard.requireRunner(jwt)
        return AutomationMaterialHeartbeatResponse(service.heartbeat(requestId, request.leaseToken))
    }

    @PostMapping("/{requestId}/fail")
    fun fail(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable requestId: UUID,
        @Valid @RequestBody request: AutomationMaterialFailureRequest,
    ): Map<String, String> {
        identityGuard.requireRunner(jwt)
        return mapOf("status" to service.fail(requestId, request.leaseToken, request.retryable).name)
    }

    @PostMapping("/{requestId}/complete", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun complete(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable requestId: UUID,
        @RequestPart metadata: AutomationMaterialCompletionMetadata,
        @RequestPart(required = false) cvDocx: MultipartFile?,
        @RequestPart(required = false) cvPdf: MultipartFile?,
        @RequestPart(required = false) coverLetter: MultipartFile?,
        @RequestPart(required = false) recruiterMessage: MultipartFile?,
    ): AutomationMaterialCompletionResponse {
        identityGuard.requireRunner(jwt)
        val files =
            mapOf(
                MaterialKind.CV_DOCX to cvDocx,
                MaterialKind.CV_PDF to cvPdf,
                MaterialKind.COVER_LETTER to coverLetter,
                MaterialKind.RECRUITER_MESSAGE to recruiterMessage,
            ).mapNotNull { (kind, file) -> file?.let { kind to it } }.toMap()
        val artifacts =
            files.mapValues { (kind, file) ->
                MaterialArtifactUpload(
                    file.bytes,
                    mediaType(kind),
                    metadata.artifactSha256[kind] ?: throw ValidationException("Missing SHA-256 for $kind"),
                )
            }
        val revision =
            service.complete(
                requestId,
                metadata.leaseToken,
                MaterialCompletion(
                    metadata.status,
                    metadata.origin,
                    metadata.generatorModel,
                    metadata.rendererVersion,
                    metadata.manifest,
                    artifacts,
                ),
            )
        return AutomationMaterialCompletionResponse(revision.id, revision.revisionNumber)
    }

    private fun mediaType(kind: MaterialKind): String =
        when (kind) {
            MaterialKind.CV_PDF -> MediaType.APPLICATION_PDF_VALUE
            MaterialKind.CV_DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            MaterialKind.COVER_LETTER, MaterialKind.RECRUITER_MESSAGE -> MediaType.TEXT_PLAIN_VALUE
        }
}
