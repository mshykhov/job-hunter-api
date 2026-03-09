package com.mshykhov.jobhunter.application.matching

import com.mshykhov.jobhunter.application.ai.ChatClientFactory
import com.mshykhov.jobhunter.application.ai.JobRelevanceEvaluator
import com.mshykhov.jobhunter.application.ai.UserAiSettingsEntity
import com.mshykhov.jobhunter.application.ai.UserAiSettingsFacade
import com.mshykhov.jobhunter.application.ai.dto.JobRelevanceResult
import com.mshykhov.jobhunter.application.job.JobEntity
import com.mshykhov.jobhunter.application.job.JobFacade
import com.mshykhov.jobhunter.application.job.JobSource
import com.mshykhov.jobhunter.application.preference.MatchingPreferences
import com.mshykhov.jobhunter.application.preference.SearchPreferences
import com.mshykhov.jobhunter.application.preference.TelegramPreferences
import com.mshykhov.jobhunter.application.preference.UserPreferenceEntity
import com.mshykhov.jobhunter.application.preference.UserPreferenceFacade
import com.mshykhov.jobhunter.application.user.UserEntity
import com.mshykhov.jobhunter.application.userjob.UserJobEntity
import com.mshykhov.jobhunter.application.userjob.UserJobFacade
import com.mshykhov.jobhunter.infrastructure.ai.AiProperties
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.client.ChatClient
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals

class JobMatchingServiceTest {
    private val jobFacade = mockk<JobFacade>()
    private val userPreferenceFacade = mockk<UserPreferenceFacade>()
    private val userJobFacade = mockk<UserJobFacade>()
    private val userAiSettingsFacade = mockk<UserAiSettingsFacade>()
    private val jobRelevanceEvaluator = mockk<JobRelevanceEvaluator>()
    private val chatClientFactory = mockk<ChatClientFactory>()
    private val aiProperties = AiProperties(matching = AiProperties.MatchingProperties(concurrency = 2))
    private val clock = Clock.fixed(Instant.parse("2026-03-09T12:00:00Z"), ZoneOffset.UTC)

    private val service =
        JobMatchingService(
            jobFacade = jobFacade,
            userPreferenceFacade = userPreferenceFacade,
            userJobFacade = userJobFacade,
            userAiSettingsFacade = userAiSettingsFacade,
            jobRelevanceEvaluator = jobRelevanceEvaluator,
            chatClientFactory = chatClientFactory,
            aiProperties = aiProperties,
            clock = clock,
        )

    @Nested
    inner class ProcessUnmatchedJobs {
        @Test
        fun `should do nothing when no unmatched jobs`() {
            every { jobFacade.findUnmatched() } returns emptyList()

            service.processUnmatchedJobs()

            verify(exactly = 0) { userPreferenceFacade.findAll() }
        }

        @Test
        fun `should mark jobs as matched when no user preferences exist`() {
            val job = testJob()
            every { jobFacade.findUnmatched() } returns listOf(job)
            every { userPreferenceFacade.findAll() } returns emptyList()
            every { jobFacade.updateMatchedAt(listOf(job.id), any()) } just Runs

            service.processUnmatchedJobs()

            verify { jobFacade.updateMatchedAt(listOf(job.id), any()) }
        }

        @Test
        fun `should create user job with cold-only reasoning when user has no AI settings`() {
            val user = UserEntity(auth0Sub = "user-1")
            val job = testJob()
            val preference = testPreference(user, matchWithAi = true)
            val savedSlot = slot<List<UserJobEntity>>()

            every { jobFacade.findUnmatched() } returns listOf(job)
            every { userPreferenceFacade.findAll() } returns listOf(preference)
            every { userAiSettingsFacade.findByUserId(user.id) } returns null
            every { userJobFacade.findByJobId(job.id) } returns emptyList()
            every { userJobFacade.saveAll(capture(savedSlot)) } answers { savedSlot.captured }
            every { jobFacade.updateMatchedAt(any(), any()) } just Runs

            service.processUnmatchedJobs()

            assertEquals(1, savedSlot.captured.size)
            assertEquals(0, savedSlot.captured[0].aiRelevanceScore)
            assertEquals("Cold filter match only — AI evaluation disabled", savedSlot.captured[0].aiReasoning)
        }

        @Test
        fun `should evaluate with AI and save result when user has AI settings`() {
            val user = UserEntity(auth0Sub = "user-1")
            val job = testJob()
            val preference = testPreference(user, matchWithAi = true)
            val aiSettings = mockk<UserAiSettingsEntity>()
            val chatClient = mockk<ChatClient>()
            val savedSlot = slot<List<UserJobEntity>>()

            every { jobFacade.findUnmatched() } returns listOf(job)
            every { userPreferenceFacade.findAll() } returns listOf(preference)
            every { userAiSettingsFacade.findByUserId(user.id) } returns aiSettings
            every { chatClientFactory.createForUser(aiSettings) } returns chatClient
            every { userJobFacade.findByJobId(job.id) } returns emptyList()
            every { jobRelevanceEvaluator.evaluate(job, preference, chatClient) } returns
                JobRelevanceResult(score = 85, reasoning = "Strong Kotlin match", inferredRemote = true)
            every { userJobFacade.saveAll(capture(savedSlot)) } answers { savedSlot.captured }
            every { jobFacade.updateMatchedAt(any(), any()) } just Runs
            every { jobFacade.updateRemote(job.id, true) } just Runs

            service.processUnmatchedJobs()

            assertEquals(1, savedSlot.captured.size)
            assertEquals(85, savedSlot.captured[0].aiRelevanceScore)
            assertEquals("Strong Kotlin match", savedSlot.captured[0].aiReasoning)
        }

        @Test
        fun `should reject job post-AI when remoteOnly and inferredRemote is false`() {
            val user = UserEntity(auth0Sub = "user-1")
            val job = testJob(remote = null)
            val preference = testPreference(user, matchWithAi = true, remoteOnly = true)
            val aiSettings = mockk<UserAiSettingsEntity>()
            val chatClient = mockk<ChatClient>()

            every { jobFacade.findUnmatched() } returns listOf(job)
            every { userPreferenceFacade.findAll() } returns listOf(preference)
            every { userAiSettingsFacade.findByUserId(user.id) } returns aiSettings
            every { chatClientFactory.createForUser(aiSettings) } returns chatClient
            every { userJobFacade.findByJobId(job.id) } returns emptyList()
            every { jobRelevanceEvaluator.evaluate(job, preference, chatClient) } returns
                JobRelevanceResult(score = 70, reasoning = "Match but not remote", inferredRemote = false)
            every { jobFacade.updateMatchedAt(any(), any()) } just Runs
            every { jobFacade.updateRemote(job.id, false) } just Runs

            service.processUnmatchedJobs()

            verify(exactly = 0) { userJobFacade.saveAll(any()) }
        }

        @Test
        fun `should skip cold-filtered jobs and not call AI`() {
            val user = UserEntity(auth0Sub = "user-1")
            val job = testJob(source = JobSource.DJINNI)
            val preference = testPreference(user, disabledSources = listOf(JobSource.DJINNI))

            every { jobFacade.findUnmatched() } returns listOf(job)
            every { userPreferenceFacade.findAll() } returns listOf(preference)
            every { userAiSettingsFacade.findByUserId(user.id) } returns null
            every { userJobFacade.findByJobId(job.id) } returns emptyList()
            every { jobFacade.updateMatchedAt(any(), any()) } just Runs

            service.processUnmatchedJobs()

            verify(exactly = 0) { jobRelevanceEvaluator.evaluate(any(), any(), any()) }
            verify(exactly = 0) { userJobFacade.saveAll(any()) }
        }
    }

    @Nested
    inner class Rematch {
        @Test
        fun `should reset matchedAt for jobs matched since given time`() {
            val job = testJob()
            val since = Instant.parse("2026-03-08T00:00:00Z")

            every { jobFacade.findMatchedSince(since) } returns listOf(job)
            every { jobFacade.updateMatchedAt(listOf(job.id), null) } just Runs

            val count = service.rematch(since)

            assertEquals(1, count)
            verify { jobFacade.updateMatchedAt(listOf(job.id), null) }
        }

        @Test
        fun `should clamp since to max 3 days ago`() {
            val veryOldSince = Instant.parse("2026-01-01T00:00:00Z")
            val expectedSince = Instant.parse("2026-03-06T12:00:00Z")

            every { jobFacade.findMatchedSince(expectedSince) } returns emptyList()

            val count = service.rematch(veryOldSince)

            assertEquals(0, count)
            verify { jobFacade.findMatchedSince(expectedSince) }
        }

        @Test
        fun `should return zero when no jobs to rematch`() {
            every { jobFacade.findMatchedSince(any()) } returns emptyList()

            val count = service.rematch(null)

            assertEquals(0, count)
            verify(exactly = 0) { jobFacade.updateMatchedAt(any(), any()) }
        }
    }

    private fun testJob(
        title: String = "Senior Kotlin Developer",
        source: JobSource = JobSource.DOU,
        remote: Boolean? = true,
    ): JobEntity =
        JobEntity(
            title = title,
            url = "https://example.com/test-job",
            description = "Looking for a Kotlin developer with Spring experience",
            source = source,
            remote = remote,
        )

    private fun testPreference(
        user: UserEntity,
        matchWithAi: Boolean = false,
        remoteOnly: Boolean = false,
        disabledSources: List<JobSource> = emptyList(),
    ): UserPreferenceEntity =
        UserPreferenceEntity(
            user = user,
            search =
                SearchPreferences(
                    remoteOnly = remoteOnly,
                    disabledSources = disabledSources,
                ),
            matching =
                MatchingPreferences(
                    matchWithAi = matchWithAi,
                    keywords = listOf("Kotlin", "Spring"),
                ),
            telegram = TelegramPreferences(),
        )
}
