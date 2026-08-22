package com.mshykhov.jobhunter.api.rest.automation

import com.fasterxml.jackson.databind.ObjectMapper
import com.mshykhov.jobhunter.application.materials.CandidateProfileService
import com.mshykhov.jobhunter.support.AbstractIntegrationTest
import com.mshykhov.jobhunter.support.SyntheticMaterialBundle
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.multipart
import java.time.Instant
import kotlin.test.assertEquals

class AutomationMaterialProfileControllerIntegrationTest : AbstractIntegrationTest() {
    @Autowired lateinit var mockMvc: MockMvc

    @Autowired lateinit var profileService: CandidateProfileService

    @Autowired lateinit var objectMapper: ObjectMapper

    @Test
    fun `runner imports profile for configured owner`() {
        val bundle = SyntheticMaterialBundle.create(objectMapper)

        mockMvc.multipart("/automation/materials/profile") {
            with(authentication(runner()))
            file(part("manifest", "manifest.json", bundle.manifest, MediaType.APPLICATION_JSON_VALUE))
            file(part("candidateProfile", "candidate-profile.json", bundle.candidateProfile, MediaType.APPLICATION_JSON_VALUE))
            file(part("factCatalog", "fact-catalog.json", bundle.factCatalog, MediaType.APPLICATION_JSON_VALUE))
            file(part("writingStyle", "writing-style.json", bundle.writingStyle, MediaType.APPLICATION_JSON_VALUE))
            file(part("baseCvDocx", "base-cv.docx", bundle.baseDocx, DOCX_MEDIA_TYPE))
            file(part("baseCvPdf", "base-cv.pdf", bundle.basePdf, MediaType.APPLICATION_PDF_VALUE))
        }.andExpect {
            status { isOk() }
            jsonPath("$.active") { value(true) }
        }

        assertEquals(1, profileService.listProfiles(OWNER_SUBJECT).size)
    }

    private fun part(name: String, filename: String, bytes: ByteArray, mediaType: String) =
        MockMultipartFile(name, filename, mediaType, bytes)

    private fun runner(): JwtAuthenticationToken {
        val jwt =
            Jwt
                .withTokenValue("test-token")
                .header("alg", "none")
                .issuer(RUNNER_ISSUER)
                .subject("runner")
                .claim("permissions", listOf("report:automation-health"))
                .issuedAt(Instant.now().minusSeconds(60))
                .expiresAt(Instant.now().plusSeconds(3600))
                .build()
        return JwtAuthenticationToken(jwt, listOf(SimpleGrantedAuthority("SCOPE_report:automation-health")))
    }

    private companion object {
        const val OWNER_SUBJECT = "local-dev-user"
        const val RUNNER_ISSUER = "http://localhost/runner"
        const val DOCX_MEDIA_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    }
}
