package com.mshykhov.jobhunter.application.materials

import com.fasterxml.jackson.databind.ObjectMapper
import com.mshykhov.jobhunter.application.job.JobGroupRepository
import com.mshykhov.jobhunter.application.job.JobRepository
import com.mshykhov.jobhunter.application.user.UserRepository
import com.mshykhov.jobhunter.application.userjob.UserJobGroupRepository
import com.mshykhov.jobhunter.support.AbstractIntegrationTest
import com.mshykhov.jobhunter.support.SyntheticMaterialBundle
import com.mshykhov.jobhunter.support.TestFixtures
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.test.assertEquals

class ApplicationMaterialServiceIntegrationTest : AbstractIntegrationTest() {
    @Autowired lateinit var service: ApplicationMaterialService

    @Autowired lateinit var profileService: CandidateProfileService

    @Autowired lateinit var objectMapper: ObjectMapper

    @Autowired lateinit var userRepository: UserRepository

    @Autowired lateinit var jobGroupRepository: JobGroupRepository

    @Autowired lateinit var jobRepository: JobRepository

    @Autowired lateinit var userJobGroupRepository: UserJobGroupRepository

    @Test
    fun `queues one idempotent request for the same vacancy and profile`() {
        val subject = "auth0|materials-${UUID.randomUUID()}"
        val user = userRepository.save(TestFixtures.userEntity(subject))
        val group = jobGroupRepository.save(TestFixtures.jobGroupEntity())
        val job = jobRepository.save(TestFixtures.jobEntity(group = group))
        userJobGroupRepository.save(TestFixtures.userJobGroupEntity(user = user, group = group))
        SyntheticMaterialBundle.create(objectMapper).also {
            profileService.importProfile(subject, it.manifest, it.candidateProfile, it.factCatalog, it.writingStyle, it.baseDocx, it.basePdf)
        }
        val kinds = setOf(MaterialKind.CV_DOCX, MaterialKind.CV_PDF, MaterialKind.COVER_LETTER)

        val first = service.ensureReady(subject, job.id, kinds, MaterialRequestMode.TERRA, CoverLetterPolicy.OPTIONAL_STANDARD, false)
        val second = service.ensureReady(subject, job.id, kinds, MaterialRequestMode.TERRA, CoverLetterPolicy.OPTIONAL_STANDARD, false)

        assertEquals(first.requestId, second.requestId)
        assertEquals(MaterialStatus.QUEUED, first.status)
        assertEquals(first.requestId, service.findForJob(subject, job.id).single().requestId)
    }
}
