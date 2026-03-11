package com.mshykhov.jobhunter.application.matching

import com.mshykhov.jobhunter.application.ai.ChatClientFactory
import com.mshykhov.jobhunter.application.ai.JobRelevanceEvaluator
import com.mshykhov.jobhunter.application.ai.UserAiSettingsEntity
import com.mshykhov.jobhunter.application.ai.UserAiSettingsFacade
import com.mshykhov.jobhunter.application.ai.dto.JobRelevanceResult
import com.mshykhov.jobhunter.application.job.JobEntity
import com.mshykhov.jobhunter.application.job.JobFacade
import com.mshykhov.jobhunter.application.job.JobGroupEntity
import com.mshykhov.jobhunter.application.job.JobGroupKeyComputer
import com.mshykhov.jobhunter.application.job.JobSource
import com.mshykhov.jobhunter.application.preference.MatchingPreferences
import com.mshykhov.jobhunter.application.preference.SearchPreferences
import com.mshykhov.jobhunter.application.preference.TelegramPreferences
import com.mshykhov.jobhunter.application.preference.UserPreferenceEntity
import com.mshykhov.jobhunter.application.preference.UserPreferenceFacade
import com.mshykhov.jobhunter.application.user.UserEntity
import com.mshykhov.jobhunter.application.userjob.UserJobGroupEntity
import com.mshykhov.jobhunter.application.userjob.UserJobGroupFacade
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
    private val userJobGroupFacade = mockk<UserJobGroupFacade>()
    private val userAiSettingsFacade = mockk<UserAiSettingsFacade>()
    private val jobRelevanceEvaluator = mockk<JobRelevanceEvaluator>()
    private val chatClientFactory = mockk<ChatClientFactory>()
    private val aiProperties = AiProperties(matching = AiProperties.MatchingProperties(concurrency = 2))
    private val clock = Clock.fixed(Instant.parse("2026-03-09T12:00:00Z"), ZoneOffset.UTC)

    private val service =
        JobMatchingService(
            jobFacade = jobFacade,
            userPreferenceFacade = userPreferenceFacade,
            userJobGroupFacade = userJobGroupFacade,
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
        fun `should create user job group with cold-only reasoning when user has no AI settings`() {
            val user = UserEntity(auth0Sub = "user-1")
            val group = testGroup()
            val job = testJob(group = group)
            val preference = testPreference(user, matchWithAi = true)
            val savedSlot = slot<List<UserJobGroupEntity>>()

            every { jobFacade.findUnmatched() } returns listOf(job)
            every { userPreferenceFacade.findAll() } returns listOf(preference)
            every { userAiSettingsFacade.findByUserId(user.id) } returns null
            every { userJobGroupFacade.findByGroupId(group.id) } returns emptyList()
            every { userJobGroupFacade.saveAll(capture(savedSlot)) } answers { savedSlot.captured }
            every { jobFacade.updateMatchedAt(any(), any()) } just Runs

            service.processUnmatchedJobs()

            assertEquals(1, savedSlot.captured.size)
            assertEquals(0, savedSlot.captured[0].aiRelevanceScore)
            assertEquals("Cold filter match only — AI evaluation disabled", savedSlot.captured[0].aiReasoning)
        }

        @Test
        fun `should evaluate with AI and save result when user has AI settings`() {
            val user = UserEntity(auth0Sub = "user-1")
            val group = testGroup()
            val job = testJob(group = group)
            val preference = testPreference(user, matchWithAi = true)
            val aiSettings = mockk<UserAiSettingsEntity>()
            val chatClient = mockk<ChatClient>()
            val savedSlot = slot<List<UserJobGroupEntity>>()

            every { jobFacade.findUnmatched() } returns listOf(job)
            every { userPreferenceFacade.findAll() } returns listOf(preference)
            every { userAiSettingsFacade.findByUserId(user.id) } returns aiSettings
            every { chatClientFactory.createForUser(aiSettings) } returns chatClient
            every { userJobGroupFacade.findByGroupId(group.id) } returns emptyList()
            every { jobRelevanceEvaluator.evaluate(job, preference, chatClient) } returns
                JobRelevanceResult(score = 85, reasoning = "Strong Kotlin match", inferredRemote = true)
            every { userJobGroupFacade.saveAll(capture(savedSlot)) } answers { savedSlot.captured }
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
            val group = testGroup()
            val job = testJob(group = group, remote = null)
            val preference = testPreference(user, matchWithAi = true, remoteOnly = true)
            val aiSettings = mockk<UserAiSettingsEntity>()
            val chatClient = mockk<ChatClient>()

            every { jobFacade.findUnmatched() } returns listOf(job)
            every { userPreferenceFacade.findAll() } returns listOf(preference)
            every { userAiSettingsFacade.findByUserId(user.id) } returns aiSettings
            every { chatClientFactory.createForUser(aiSettings) } returns chatClient
            every { userJobGroupFacade.findByGroupId(group.id) } returns emptyList()
            every { jobRelevanceEvaluator.evaluate(job, preference, chatClient) } returns
                JobRelevanceResult(score = 70, reasoning = "Match but not remote", inferredRemote = false)
            every { jobFacade.updateMatchedAt(any(), any()) } just Runs
            every { jobFacade.updateRemote(job.id, false) } just Runs

            service.processUnmatchedJobs()

            verify(exactly = 0) { userJobGroupFacade.saveAll(any()) }
        }

        @Test
        fun `should skip cold-filtered jobs and not call AI`() {
            val user = UserEntity(auth0Sub = "user-1")
            val group = testGroup()
            val job = testJob(group = group, source = JobSource.DJINNI)
            val preference = testPreference(user, disabledSources = listOf(JobSource.DJINNI))

            every { jobFacade.findUnmatched() } returns listOf(job)
            every { userPreferenceFacade.findAll() } returns listOf(preference)
            every { userAiSettingsFacade.findByUserId(user.id) } returns null
            every { userJobGroupFacade.findByGroupId(group.id) } returns emptyList()
            every { jobFacade.updateMatchedAt(any(), any()) } just Runs

            service.processUnmatchedJobs()

            verify(exactly = 0) { jobRelevanceEvaluator.evaluate(any(), any(), any()) }
            verify(exactly = 0) { userJobGroupFacade.saveAll(any()) }
        }
    }

    @Nested
    inner class GroupMatching {
        @Test
        fun `should select job with longest description as representative`() {
            val user = UserEntity(auth0Sub = "user-1")
            val group = testGroup()
            val shortJob =
                testJob(
                    group = group,
                    title = "Senior Kotlin Developer",
                ).apply { description = "Short" }
            val longJob =
                testJob(
                    group = group,
                    title = "Senior Kotlin Developer",
                ).apply { description = "This is a much longer description for the Kotlin developer position" }
            val preference = testPreference(user, matchWithAi = true)
            val aiSettings = mockk<UserAiSettingsEntity>()
            val chatClient = mockk<ChatClient>()
            val savedSlot = slot<List<UserJobGroupEntity>>()

            every { jobFacade.findUnmatched() } returns listOf(shortJob, longJob)
            every { userPreferenceFacade.findAll() } returns listOf(preference)
            every { userAiSettingsFacade.findByUserId(user.id) } returns aiSettings
            every { chatClientFactory.createForUser(aiSettings) } returns chatClient
            every { userJobGroupFacade.findByGroupId(group.id) } returns emptyList()
            every { jobRelevanceEvaluator.evaluate(longJob, preference, chatClient) } returns
                JobRelevanceResult(score = 90, reasoning = "Great match", inferredRemote = true)
            every { userJobGroupFacade.saveAll(capture(savedSlot)) } answers { savedSlot.captured }
            every { jobFacade.updateMatchedAt(any(), any()) } just Runs
            every { jobFacade.updateRemote(longJob.id, true) } just Runs

            service.processUnmatchedJobs()

            verify { jobRelevanceEvaluator.evaluate(longJob, preference, chatClient) }
            verify(exactly = 0) { jobRelevanceEvaluator.evaluate(shortJob, any(), any()) }
        }

        @Test
        fun `should match one group to multiple users`() {
            val user1 = UserEntity(auth0Sub = "user-1")
            val user2 = UserEntity(auth0Sub = "user-2")
            val group = testGroup()
            val job = testJob(group = group)
            val preference1 = testPreference(user1, matchWithAi = false)
            val preference2 = testPreference(user2, matchWithAi = false)
            val savedSlot = slot<List<UserJobGroupEntity>>()

            every { jobFacade.findUnmatched() } returns listOf(job)
            every { userPreferenceFacade.findAll() } returns listOf(preference1, preference2)
            every { userAiSettingsFacade.findByUserId(any()) } returns null
            every { userJobGroupFacade.findByGroupId(group.id) } returns emptyList()
            every { userJobGroupFacade.saveAll(capture(savedSlot)) } answers { savedSlot.captured }
            every { jobFacade.updateMatchedAt(any(), any()) } just Runs

            service.processUnmatchedJobs()

            assertEquals(2, savedSlot.captured.size)
            val userIds = savedSlot.captured.map { it.user.id }.toSet()
            assertEquals(setOf(user1.id, user2.id), userIds)
        }

        @Test
        fun `should skip user who already has this group matched`() {
            val user = UserEntity(auth0Sub = "user-1")
            val group = testGroup()
            val job = testJob(group = group)
            val preference = testPreference(user, matchWithAi = false)
            val existingUserJobGroup =
                UserJobGroupEntity(
                    user = user,
                    group = group,
                    aiRelevanceScore = 50,
                    aiReasoning = "Old reasoning",
                )
            val savedSlot = slot<List<UserJobGroupEntity>>()

            every { jobFacade.findUnmatched() } returns listOf(job)
            every { userPreferenceFacade.findAll() } returns listOf(preference)
            every { userAiSettingsFacade.findByUserId(user.id) } returns null
            every { userJobGroupFacade.findByGroupId(group.id) } returns listOf(existingUserJobGroup)
            every { userJobGroupFacade.saveAll(capture(savedSlot)) } answers { savedSlot.captured }
            every { jobFacade.updateMatchedAt(any(), any()) } just Runs

            service.processUnmatchedJobs()

            assertEquals(1, savedSlot.captured.size)
            assertEquals(0, savedSlot.captured[0].aiRelevanceScore)
            assertEquals("Cold filter match only — AI evaluation disabled", savedSlot.captured[0].aiReasoning)
        }

        @Test
        fun `should mark all jobs in group as matched even when no preferences pass`() {
            val user = UserEntity(auth0Sub = "user-1")
            val group = testGroup()
            val job1 = testJob(group = group, source = JobSource.DJINNI)
            val job2 =
                JobEntity(
                    title = "Senior Kotlin Developer",
                    group = group,
                    url = "https://example.com/test-job-2",
                    description = "Another listing",
                    source = JobSource.DJINNI,
                    remote = true,
                )
            val preference = testPreference(user, disabledSources = listOf(JobSource.DJINNI))

            every { jobFacade.findUnmatched() } returns listOf(job1, job2)
            every { userPreferenceFacade.findAll() } returns listOf(preference)
            every { userAiSettingsFacade.findByUserId(user.id) } returns null
            every { userJobGroupFacade.findByGroupId(group.id) } returns emptyList()
            every { jobFacade.updateMatchedAt(listOf(job1.id, job2.id), any()) } just Runs

            service.processUnmatchedJobs()

            verify { jobFacade.updateMatchedAt(listOf(job1.id, job2.id), any()) }
            verify(exactly = 0) { userJobGroupFacade.saveAll(any()) }
        }

        @Test
        fun `should handle AI evaluation failure gracefully and not save group`() {
            val user = UserEntity(auth0Sub = "user-1")
            val group = testGroup()
            val job = testJob(group = group)
            val preference = testPreference(user, matchWithAi = true)
            val aiSettings = mockk<UserAiSettingsEntity>()
            val chatClient = mockk<ChatClient>()

            every { jobFacade.findUnmatched() } returns listOf(job)
            every { userPreferenceFacade.findAll() } returns listOf(preference)
            every { userAiSettingsFacade.findByUserId(user.id) } returns aiSettings
            every { chatClientFactory.createForUser(aiSettings) } returns chatClient
            every { userJobGroupFacade.findByGroupId(group.id) } returns emptyList()
            every { jobRelevanceEvaluator.evaluate(job, preference, chatClient) } throws RuntimeException("API error")
            every { jobFacade.updateMatchedAt(any(), any()) } just Runs

            service.processUnmatchedJobs()

            verify(exactly = 0) { userJobGroupFacade.saveAll(any()) }
            verify { jobFacade.updateMatchedAt(listOf(job.id), any()) }
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

    private fun testGroup(
        title: String = "Senior Kotlin Developer",
        company: String? = null,
    ): JobGroupEntity =
        JobGroupEntity(
            groupKey = JobGroupKeyComputer.compute(title, company),
            title = title,
            company = company,
        )

    private fun testJob(
        title: String = "Senior Kotlin Developer",
        source: JobSource = JobSource.DOU,
        remote: Boolean? = true,
        group: JobGroupEntity = testGroup(title),
    ): JobEntity =
        JobEntity(
            title = title,
            group = group,
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
