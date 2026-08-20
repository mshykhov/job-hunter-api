package com.mshykhov.jobhunter.application.materials

import com.fasterxml.jackson.databind.ObjectMapper
import com.mshykhov.jobhunter.support.AbstractIntegrationTest
import com.mshykhov.jobhunter.support.SyntheticMaterialBundle
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CandidateProfileServiceIntegrationTest : AbstractIntegrationTest() {
    @Autowired lateinit var service: CandidateProfileService

    @Autowired lateinit var objectMapper: ObjectMapper

    @Test
    fun `imports an immutable encrypted bundle and activates it idempotently`() {
        val subject = "auth0|profile-${UUID.randomUUID()}"
        val bundle = SyntheticMaterialBundle.create(objectMapper)

        val first = bundle.importFor(subject)
        val second = bundle.importFor(subject)

        assertEquals(first.id, second.id)
        assertTrue(second.active)
        assertEquals(listOf(second), service.listProfiles(subject))
    }

    private fun SyntheticMaterialBundle.importFor(subject: String) =
        service.importProfile(subject, manifest, candidateProfile, factCatalog, writingStyle, baseDocx, basePdf)
}
