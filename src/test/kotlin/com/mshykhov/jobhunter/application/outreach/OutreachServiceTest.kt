package com.mshykhov.jobhunter.application.outreach

import com.mshykhov.jobhunter.application.ai.ChatClientFactory
import com.mshykhov.jobhunter.application.ai.OutreachGenerator
import com.mshykhov.jobhunter.application.ai.UserAiSettingsEntity
import com.mshykhov.jobhunter.application.ai.UserAiSettingsService
import com.mshykhov.jobhunter.application.common.AiNotConfiguredException
import com.mshykhov.jobhunter.application.common.NotFoundException
import com.mshykhov.jobhunter.application.job.JobFacade
import com.mshykhov.jobhunter.application.job.JobSource
import com.mshykhov.jobhunter.application.preference.UserPreferenceFacade
import com.mshykhov.jobhunter.application.user.UserFacade
import com.mshykhov.jobhunter.application.userjob.UserJobFacade
import com.mshykhov.jobhunter.support.TestFixtures
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.ai.chat.client.ChatClient
import kotlin.test.assertEquals

class OutreachServiceTest {
    private val userFacade = mockk<UserFacade>()
    private val userJobFacade = mockk<UserJobFacade>()
    private val jobFacade = mockk<JobFacade>()
    private val userPreferenceFacade = mockk<UserPreferenceFacade>()
    private val outreachSettingsFacade = mockk<OutreachSettingsFacade>()
    private val outreachGenerator = mockk<OutreachGenerator>()
    private val userAiSettingsService = mockk<UserAiSettingsService>()
    private val chatClientFactory = mockk<ChatClientFactory>()

    private val service =
        OutreachService(
            userFacade = userFacade,
            userJobFacade = userJobFacade,
            jobFacade = jobFacade,
            userPreferenceFacade = userPreferenceFacade,
            outreachSettingsFacade = outreachSettingsFacade,
            outreachGenerator = outreachGenerator,
            userAiSettingsService = userAiSettingsService,
            chatClientFactory = chatClientFactory,
        )

    private val auth0Sub = "auth0|test-user"

    @Nested
    inner class GenerateCoverLetter {
        @Test
        fun `should generate cover letter and save to user job`() {
            val user = TestFixtures.userEntity(auth0Sub)
            val job = TestFixtures.jobEntity()
            val userJob = TestFixtures.userJobEntity(user = user, job = job)
            val aiSettings = mockk<UserAiSettingsEntity>()
            val chatClient = mockk<ChatClient>()

            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userJobFacade.findOrCreateForGroupMember(user, job.id) } returns userJob
            every { outreachSettingsFacade.findByUserId(user.id) } returns null
            every { userPreferenceFacade.findByUserId(user.id) } returns null
            every { userAiSettingsService.resolveForUser(auth0Sub) } returns aiSettings
            every { chatClientFactory.createForUser(aiSettings) } returns chatClient
            every {
                outreachGenerator.generateCoverLetter(job, null, null, null, chatClient)
            } returns "Generated cover letter"
            every { userJobFacade.save(userJob) } returns userJob

            val result = service.generateCoverLetter(auth0Sub, job.id)

            assertEquals("Generated cover letter", result.coverLetter)
            assertEquals("Generated cover letter", userJob.coverLetter)
            verify { userJobFacade.save(userJob) }
        }

        @Test
        fun `should use outreach settings when available`() {
            val user = TestFixtures.userEntity(auth0Sub)
            val job = TestFixtures.jobEntity(source = JobSource.DJINNI)
            val userJob = TestFixtures.userJobEntity(user = user, job = job)
            val aiSettings = mockk<UserAiSettingsEntity>()
            val chatClient = mockk<ChatClient>()
            val sourceConfig = OutreachSourceConfig(coverLetterPrompt = "Custom prompt")
            val settings =
                OutreachSettingsEntity(
                    user = user,
                    coverLetterPrompt = "Default custom",
                    sourceConfig = mapOf(JobSource.DJINNI to sourceConfig),
                )

            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userJobFacade.findOrCreateForGroupMember(user, job.id) } returns userJob
            every { outreachSettingsFacade.findByUserId(user.id) } returns settings
            every { userPreferenceFacade.findByUserId(user.id) } returns null
            every { userAiSettingsService.resolveForUser(auth0Sub) } returns aiSettings
            every { chatClientFactory.createForUser(aiSettings) } returns chatClient
            every {
                outreachGenerator.generateCoverLetter(job, null, sourceConfig, "Default custom", chatClient)
            } returns "Custom cover letter"
            every { userJobFacade.save(userJob) } returns userJob

            val result = service.generateCoverLetter(auth0Sub, job.id)

            assertEquals("Custom cover letter", result.coverLetter)
        }

        @Test
        fun `should throw NotFoundException when user not found`() {
            every { userFacade.findByAuth0Sub(auth0Sub) } returns null

            assertThrows<NotFoundException> {
                service.generateCoverLetter(auth0Sub, TestFixtures.jobEntity().id)
            }
        }

        @Test
        fun `should throw NotFoundException when job not in user matches`() {
            val user = TestFixtures.userEntity(auth0Sub)
            val jobId = TestFixtures.jobEntity().id

            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userJobFacade.findOrCreateForGroupMember(user, jobId) } throws
                NotFoundException("Job not found in your matches")

            assertThrows<NotFoundException> {
                service.generateCoverLetter(auth0Sub, jobId)
            }
        }

        @Test
        fun `should throw AiNotConfiguredException when AI not configured`() {
            val user = TestFixtures.userEntity(auth0Sub)
            val job = TestFixtures.jobEntity()
            val userJob = TestFixtures.userJobEntity(user = user, job = job)

            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userJobFacade.findOrCreateForGroupMember(user, job.id) } returns userJob
            every { outreachSettingsFacade.findByUserId(user.id) } returns null
            every { userPreferenceFacade.findByUserId(user.id) } returns null
            every { userAiSettingsService.resolveForUser(auth0Sub) } throws AiNotConfiguredException()

            assertThrows<AiNotConfiguredException> {
                service.generateCoverLetter(auth0Sub, job.id)
            }
        }
    }

    @Nested
    inner class GenerateRecruiterMessage {
        @Test
        fun `should generate recruiter message and save to user job`() {
            val user = TestFixtures.userEntity(auth0Sub)
            val job = TestFixtures.jobEntity()
            val userJob = TestFixtures.userJobEntity(user = user, job = job)
            val aiSettings = mockk<UserAiSettingsEntity>()
            val chatClient = mockk<ChatClient>()

            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userJobFacade.findOrCreateForGroupMember(user, job.id) } returns userJob
            every { outreachSettingsFacade.findByUserId(user.id) } returns null
            every { userPreferenceFacade.findByUserId(user.id) } returns null
            every { userAiSettingsService.resolveForUser(auth0Sub) } returns aiSettings
            every { chatClientFactory.createForUser(aiSettings) } returns chatClient
            every {
                outreachGenerator.generateRecruiterMessage(job, null, null, null, chatClient)
            } returns "Generated recruiter message"
            every { userJobFacade.save(userJob) } returns userJob

            val result = service.generateRecruiterMessage(auth0Sub, job.id)

            assertEquals("Generated recruiter message", result.recruiterMessage)
            assertEquals("Generated recruiter message", userJob.recruiterMessage)
            verify { userJobFacade.save(userJob) }
        }

        @Test
        fun `should throw NotFoundException when user not found`() {
            every { userFacade.findByAuth0Sub(auth0Sub) } returns null

            assertThrows<NotFoundException> {
                service.generateRecruiterMessage(auth0Sub, TestFixtures.jobEntity().id)
            }
        }

        @Test
        fun `should throw NotFoundException when job not in user matches`() {
            val user = TestFixtures.userEntity(auth0Sub)
            val jobId = TestFixtures.jobEntity().id

            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userJobFacade.findOrCreateForGroupMember(user, jobId) } throws
                NotFoundException("Job not found in your matches")

            assertThrows<NotFoundException> {
                service.generateRecruiterMessage(auth0Sub, jobId)
            }
        }

        @Test
        fun `should throw AiNotConfiguredException when AI not configured`() {
            val user = TestFixtures.userEntity(auth0Sub)
            val job = TestFixtures.jobEntity()
            val userJob = TestFixtures.userJobEntity(user = user, job = job)

            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userJobFacade.findOrCreateForGroupMember(user, job.id) } returns userJob
            every { outreachSettingsFacade.findByUserId(user.id) } returns null
            every { userPreferenceFacade.findByUserId(user.id) } returns null
            every { userAiSettingsService.resolveForUser(auth0Sub) } throws AiNotConfiguredException()

            assertThrows<AiNotConfiguredException> {
                service.generateRecruiterMessage(auth0Sub, job.id)
            }
        }
    }

    @Nested
    inner class TestCoverLetter {
        @Test
        fun `should generate test cover letter without saving`() {
            val user = TestFixtures.userEntity(auth0Sub)
            val job = TestFixtures.jobEntity(source = JobSource.DOU)
            val aiSettings = mockk<UserAiSettingsEntity>()
            val chatClient = mockk<ChatClient>()

            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { jobFacade.findTopBySourceOrderByCreatedAtDesc(JobSource.DOU) } returns job
            every { outreachSettingsFacade.findByUserId(user.id) } returns null
            every { userPreferenceFacade.findByUserId(user.id) } returns null
            every { userAiSettingsService.resolveForUser(auth0Sub) } returns aiSettings
            every { chatClientFactory.createForUser(aiSettings) } returns chatClient
            every {
                outreachGenerator.generateCoverLetter(job, null, null, null, chatClient)
            } returns "Test cover letter"

            val result = service.testCoverLetter(auth0Sub, JobSource.DOU)

            assertEquals("Test cover letter", result.coverLetter)
            verify(exactly = 0) { userJobFacade.save(any()) }
        }

        @Test
        fun `should throw NotFoundException when no jobs for source`() {
            val user = TestFixtures.userEntity(auth0Sub)

            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { jobFacade.findTopBySourceOrderByCreatedAtDesc(JobSource.WEB3CAREER) } returns null

            assertThrows<NotFoundException> {
                service.testCoverLetter(auth0Sub, JobSource.WEB3CAREER)
            }
        }
    }

    @Nested
    inner class TestRecruiterMessage {
        @Test
        fun `should generate test recruiter message without saving`() {
            val user = TestFixtures.userEntity(auth0Sub)
            val job = TestFixtures.jobEntity(source = JobSource.DOU)
            val aiSettings = mockk<UserAiSettingsEntity>()
            val chatClient = mockk<ChatClient>()

            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { jobFacade.findTopBySourceOrderByCreatedAtDesc(JobSource.DOU) } returns job
            every { outreachSettingsFacade.findByUserId(user.id) } returns null
            every { userPreferenceFacade.findByUserId(user.id) } returns null
            every { userAiSettingsService.resolveForUser(auth0Sub) } returns aiSettings
            every { chatClientFactory.createForUser(aiSettings) } returns chatClient
            every {
                outreachGenerator.generateRecruiterMessage(job, null, null, null, chatClient)
            } returns "Test recruiter message"

            val result = service.testRecruiterMessage(auth0Sub, JobSource.DOU)

            assertEquals("Test recruiter message", result.recruiterMessage)
            verify(exactly = 0) { userJobFacade.save(any()) }
        }
    }
}
