package com.mshykhov.jobhunter.application.materials

import com.fasterxml.jackson.databind.ObjectMapper
import com.mshykhov.jobhunter.application.job.JobGroupRepository
import com.mshykhov.jobhunter.application.job.JobRepository
import com.mshykhov.jobhunter.application.user.UserRepository
import com.mshykhov.jobhunter.application.userjob.UserJobGroupRepository
import com.mshykhov.jobhunter.infrastructure.materials.EncryptedMaterialStore
import com.mshykhov.jobhunter.support.AbstractIntegrationTest
import com.mshykhov.jobhunter.support.SyntheticMaterialBundle
import com.mshykhov.jobhunter.support.TestFixtures
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MaterialLeaseServiceIntegrationTest : AbstractIntegrationTest() {
    @Autowired lateinit var materialService: ApplicationMaterialService

    @Autowired lateinit var leaseService: MaterialLeaseService

    @Autowired lateinit var profileService: CandidateProfileService

    @Autowired lateinit var objectMapper: ObjectMapper

    @Autowired lateinit var userRepository: UserRepository

    @Autowired lateinit var jobGroupRepository: JobGroupRepository

    @Autowired lateinit var jobRepository: JobRepository

    @Autowired lateinit var userJobGroupRepository: UserJobGroupRepository

    @Test
    fun `claims once and completes an immutable ready revision`() {
        val subject = "auth0|lease-${UUID.randomUUID()}"
        val user = userRepository.save(TestFixtures.userEntity(subject))
        val group = jobGroupRepository.save(TestFixtures.jobGroupEntity(title = "Lease ${UUID.randomUUID()}"))
        val job = jobRepository.save(TestFixtures.jobEntity(group = group))
        userJobGroupRepository.save(TestFixtures.userJobGroupEntity(user = user, group = group))
        SyntheticMaterialBundle.create(objectMapper).also {
            profileService.importProfile(subject, it.manifest, it.candidateProfile, it.factCatalog, it.writingStyle, it.baseDocx, it.basePdf)
        }
        val kinds = setOf(MaterialKind.CV_DOCX, MaterialKind.CV_PDF, MaterialKind.COVER_LETTER)
        val request = materialService.ensureReady(subject, job.id, kinds, MaterialRequestMode.TERRA, CoverLetterPolicy.OPTIONAL_STANDARD, false)

        val claim = claimRequest(request.requestId)
        assertNull(leaseService.claim("other-worker"))
        leaseService.heartbeat(claim.requestId, claim.leaseToken)
        val docx = "tailored docx".toByteArray()
        val pdf = "%PDF tailored".toByteArray()
        val coverLetter = "I build Kotlin backends that match this role. I would be glad to discuss your product and team.".toByteArray()
        val completion =
            MaterialCompletion(
                MaterialStatus.READY,
                MaterialOrigin.GENERATED,
                "gpt-5.6-terra",
                "cv-materials/test",
                mapOf("schemaVersion" to "application-materials/v1"),
                mapOf(
                    MaterialKind.CV_DOCX to upload(docx, "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
                    MaterialKind.CV_PDF to upload(pdf, "application/pdf"),
                    MaterialKind.COVER_LETTER to upload(coverLetter, "text/plain"),
                ),
            )

        val revision = leaseService.complete(claim.requestId, claim.leaseToken, completion)
        val retried = leaseService.complete(claim.requestId, claim.leaseToken, completion)

        assertEquals(revision.id, retried.id)
        assertEquals(1, revision.revisionNumber)
        assertEquals(MaterialStatus.READY, materialService.findForJob(subject, job.id).single().status)
        val revisionView = materialService.findRevisions(subject, job.id).single()
        assertEquals(revision.id, revisionView.id)
        assertEquals(true, revisionView.selected)
        assertContentEquals(pdf, materialService.downloadArtifact(subject, revision.id, MaterialKind.CV_PDF).content)
        val improvement = materialService.improveWithSol(subject, revision.id)
        assertEquals(MaterialRequestMode.SOL_IMPROVE, improvement.mode)
        assertEquals(MaterialStatus.QUEUED, improvement.status)
    }

    @Test
    fun `inherits unchanged artifacts when one material is regenerated`() {
        val subject = "auth0|lease-partial-${UUID.randomUUID()}"
        val user = userRepository.save(TestFixtures.userEntity(subject))
        val group = jobGroupRepository.save(TestFixtures.jobGroupEntity(title = "Partial ${UUID.randomUUID()}"))
        val job = jobRepository.save(TestFixtures.jobEntity(group = group))
        userJobGroupRepository.save(TestFixtures.userJobGroupEntity(user = user, group = group))
        SyntheticMaterialBundle.create(objectMapper).also {
            profileService.importProfile(subject, it.manifest, it.candidateProfile, it.factCatalog, it.writingStyle, it.baseDocx, it.basePdf)
        }
        val initialRequest =
            materialService.ensureReady(
                subject,
                job.id,
                setOf(MaterialKind.CV_DOCX, MaterialKind.CV_PDF, MaterialKind.COVER_LETTER, MaterialKind.RECRUITER_MESSAGE),
                MaterialRequestMode.TERRA,
                CoverLetterPolicy.OPTIONAL_STANDARD,
                false,
            )
        val initialClaim = claimRequest(initialRequest.requestId)
        val originalPdf = "%PDF original".toByteArray()
        leaseService.complete(
            initialClaim.requestId,
            initialClaim.leaseToken,
            MaterialCompletion(
                MaterialStatus.READY,
                MaterialOrigin.GENERATED,
                "gpt-5.6-terra",
                "cv-materials/test",
                emptyMap(),
                mapOf(
                    MaterialKind.CV_DOCX to upload("original docx".toByteArray(), "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
                    MaterialKind.CV_PDF to upload(originalPdf, "application/pdf"),
                    MaterialKind.COVER_LETTER to upload("Original cover letter".toByteArray(), "text/plain"),
                    MaterialKind.RECRUITER_MESSAGE to upload("Original recruiter message".toByteArray(), "text/plain"),
                ),
            ),
        )

        val partialRequest =
            materialService.ensureReady(
                subject,
                job.id,
                setOf(MaterialKind.COVER_LETTER),
                MaterialRequestMode.TERRA,
                CoverLetterPolicy.OPTIONAL_STANDARD,
                true,
            )
        val partialClaim = claimRequest(partialRequest.requestId)
        val newCoverLetter = "A shorter human cover letter".toByteArray()
        val revision =
            leaseService.complete(
                partialClaim.requestId,
                partialClaim.leaseToken,
                MaterialCompletion(
                    MaterialStatus.READY,
                    MaterialOrigin.GENERATED,
                    "gpt-5.6-terra",
                    "cv-materials/test",
                    emptyMap(),
                    mapOf(MaterialKind.COVER_LETTER to upload(newCoverLetter, "text/plain")),
                ),
            )

        assertContentEquals(originalPdf, materialService.downloadArtifact(subject, revision.id, MaterialKind.CV_PDF).content)
        assertContentEquals(newCoverLetter, materialService.downloadArtifact(subject, revision.id, MaterialKind.COVER_LETTER).content)
        assertEquals(4, materialService.findRevisions(subject, job.id).first { it.id == revision.id }.artifacts.size)
    }

    private fun upload(content: ByteArray, mediaType: String) =
        MaterialArtifactUpload(content, mediaType, EncryptedMaterialStore.sha256(content))

    private fun claimRequest(requestId: UUID): MaterialClaim {
        repeat(10) {
            val claim = assertNotNull(leaseService.claim("test-worker"))
            if (claim.requestId == requestId) return claim
            leaseService.fail(claim.requestId, claim.leaseToken, retryable = false)
        }
        error("Material request $requestId was not claimable")
    }
}
