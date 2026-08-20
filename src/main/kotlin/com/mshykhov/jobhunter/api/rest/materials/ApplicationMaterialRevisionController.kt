package com.mshykhov.jobhunter.api.rest.materials

import com.mshykhov.jobhunter.api.rest.materials.dto.ApplicationMaterialRequestResponse
import com.mshykhov.jobhunter.application.materials.ApplicationMaterialService
import com.mshykhov.jobhunter.application.materials.MaterialKind
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/materials/revisions/{revisionId}")
class ApplicationMaterialRevisionController(private val service: ApplicationMaterialService) {
    @GetMapping("/artifacts/{kind}")
    @PreAuthorize("hasAuthority('SCOPE_read:jobs')")
    fun download(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable revisionId: UUID,
        @PathVariable kind: MaterialKind,
    ): ResponseEntity<ByteArray> {
        val artifact = service.downloadArtifact(jwt.subject, revisionId, kind)
        return ResponseEntity
            .ok()
            .contentType(MediaType.parseMediaType(artifact.mediaType))
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(artifact.filename).build().toString())
            .body(artifact.content)
    }

    @PostMapping("/improve-with-sol")
    @PreAuthorize("hasAuthority('SCOPE_write:jobs')")
    fun improveWithSol(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable revisionId: UUID,
    ): ApplicationMaterialRequestResponse = ApplicationMaterialRequestResponse.from(service.improveWithSol(jwt.subject, revisionId))

    @PostMapping("/select")
    @PreAuthorize("hasAuthority('SCOPE_write:jobs')")
    fun select(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable revisionId: UUID,
    ): ResponseEntity<Void> {
        service.selectRevision(jwt.subject, revisionId)
        return ResponseEntity.noContent().build()
    }
}
