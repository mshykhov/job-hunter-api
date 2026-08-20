package com.mshykhov.jobhunter.application.matching

import com.mshykhov.jobhunter.application.job.JobEntity
import com.mshykhov.jobhunter.application.job.JobGroupEntity
import com.mshykhov.jobhunter.application.job.JobGroupKeyComputer
import com.mshykhov.jobhunter.application.job.JobSource
import com.mshykhov.jobhunter.application.preference.MatchingPreferences
import com.mshykhov.jobhunter.application.preference.PreferenceChangedEvent
import com.mshykhov.jobhunter.application.preference.SearchPreferences
import com.mshykhov.jobhunter.application.preference.TelegramPreferences
import com.mshykhov.jobhunter.application.preference.UserPreferenceEntity
import com.mshykhov.jobhunter.application.preference.UserPreferenceFacade
import com.mshykhov.jobhunter.application.statistics.DecisionOutcome
import com.mshykhov.jobhunter.application.statistics.UserJobGroupDecisionFacade
import com.mshykhov.jobhunter.application.user.UserEntity
import com.mshykhov.jobhunter.application.userjob.UserJobGroupEntity
import com.mshykhov.jobhunter.application.userjob.UserJobGroupFacade
import com.mshykhov.jobhunter.application.userjob.UserJobStatus
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.scheduling.annotation.Async
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ColdFilterRetroServiceTest {
    private val userPreferenceFacade = mockk<UserPreferenceFacade>()
    private val userJobGroupFacade = mockk<UserJobGroupFacade>()
    private val decisionFacade = mockk<UserJobGroupDecisionFacade>(relaxed = true)
    private val entityManager = mockk<EntityManager>()

    private val service = ColdFilterRetroService(userPreferenceFacade, userJobGroupFacade, decisionFacade, entityManager)

    @Nested
    inner class Filtering {
        @Test
        fun `should retain NEW groups and record cold rejection after preference change`() {
            val user = UserEntity(auth0Sub = "user-1")
            val leadUserGroup = userJobGroup(user, groupWithJob("Java Team Lead"))
            val devUserGroup = userJobGroup(user, groupWithJob("Senior Java Developer"))
            val preference = preference(user, excludedTitleKeywords = listOf("lead"))
            stubGroups(user, preference, listOf(leadUserGroup, devUserGroup))
            every { userJobGroupFacade.deleteByIdsAndUserIdAndStatus(listOf(leadUserGroup.id), user.id, UserJobStatus.NEW) } returns 1

            service.onPreferenceChanged(PreferenceChangedEvent(user.id))

            verify {
                decisionFacade.upsert(
                    user,
                    leadUserGroup.group,
                    leadUserGroup.group.jobs,
                    DecisionOutcome.COLD_REJECTED,
                    "excludedTitleKeyword",
                    null,
                    null,
                )
            }
            verify { userJobGroupFacade.deleteByIdsAndUserIdAndStatus(listOf(leadUserGroup.id), user.id, UserJobStatus.NEW) }
        }

        @Test
        fun `should not delete anything when all groups still pass the filter`() {
            val user = UserEntity(auth0Sub = "user-1")
            val devUserGroup = userJobGroup(user, groupWithJob("Senior Java Developer"))
            val preference = preference(user, excludedTitleKeywords = listOf("lead"))

            stubGroups(user, preference, listOf(devUserGroup))

            service.onPreferenceChanged(PreferenceChangedEvent(user.id))

            verify(exactly = 0) { userJobGroupFacade.deleteByIdsAndUserIdAndStatus(any(), any(), any()) }
        }

        @Test
        fun `should do nothing when user has no preference`() {
            val user = UserEntity(auth0Sub = "user-1")

            every { userPreferenceFacade.findByUserId(user.id) } returns null

            service.onPreferenceChanged(PreferenceChangedEvent(user.id))

            verify(exactly = 0) { userJobGroupFacade.findIdsByUserIdAndStatus(any(), any()) }
        }

        @Test
        fun `should evaluate filter against representative job with longest description`() {
            val user = UserEntity(auth0Sub = "user-1")
            val group =
                groupWithJobs(
                    "Senior Java Developer",
                    job("Senior Java Developer", description = "short crypto mention"),
                    job("Senior Java Developer", description = "much longer clean description about java and spring boot"),
                )
            val userGroup = userJobGroup(user, group)
            val preference = preference(user, excludedKeywords = listOf("crypto"))

            stubGroups(user, preference, listOf(userGroup))

            service.onPreferenceChanged(PreferenceChangedEvent(user.id))

            verify(exactly = 0) { userJobGroupFacade.deleteByIdsAndUserIdAndStatus(any(), any(), any()) }
        }

        @Test
        fun `should keep group without jobs`() {
            val user = UserEntity(auth0Sub = "user-1")
            val emptyGroup = group("Senior Java Developer")
            val userGroup = userJobGroup(user, emptyGroup)
            val preference = preference(user, excludedTitleKeywords = listOf("lead"))

            stubGroups(user, preference, listOf(userGroup))

            service.onPreferenceChanged(PreferenceChangedEvent(user.id))

            verify(exactly = 0) { userJobGroupFacade.deleteByIdsAndUserIdAndStatus(any(), any(), any()) }
        }
    }

    @Test
    fun `should load and release persistence state in bounded chunks`() {
        val user = UserEntity(auth0Sub = "user-1")
        val preference = preference(user)
        val groups =
            List(COLD_FILTER_RETRO_BATCH_SIZE + 1) { index ->
                userJobGroup(user, group("Java Developer $index"))
            }
        val groupsById = groups.associateBy(UserJobGroupEntity::getId)
        val fetchedChunks = mutableListOf<List<UUID>>()

        every { userPreferenceFacade.findByUserId(user.id) } returns preference
        every { userJobGroupFacade.findIdsByUserIdAndStatus(user.id, UserJobStatus.NEW) } returns groupsById.keys.toList()
        every {
            userJobGroupFacade.findByIdsWithGroupAndJobs(user.id, UserJobStatus.NEW, capture(fetchedChunks))
        } answers {
            thirdArg<List<UUID>>().map(groupsById::getValue)
        }
        every { userJobGroupFacade.flush() } just Runs
        every { entityManager.clear() } just Runs

        service.onPreferenceChanged(PreferenceChangedEvent(user.id))

        assertEquals(listOf(COLD_FILTER_RETRO_BATCH_SIZE, 1), fetchedChunks.map { it.size })
        verify(exactly = 2) { userJobGroupFacade.flush() }
        verify(exactly = 2) { entityManager.clear() }
    }

    @Test
    fun `should schedule listener on dedicated executor after commit`() {
        val method = ColdFilterRetroService::class.java.getDeclaredMethod("onPreferenceChanged", PreferenceChangedEvent::class.java)

        val async = assertNotNull(method.getAnnotation(Async::class.java))
        val transactionalEventListener = assertNotNull(method.getAnnotation(TransactionalEventListener::class.java))

        assertEquals(COLD_FILTER_RETRO_EXECUTOR, async.value)
        assertEquals(TransactionPhase.AFTER_COMMIT, transactionalEventListener.phase)
    }

    private fun stubGroups(
        user: UserEntity,
        preference: UserPreferenceEntity,
        groups: List<UserJobGroupEntity>,
    ) {
        val ids = groups.map(UserJobGroupEntity::getId)
        every { userPreferenceFacade.findByUserId(user.id) } returns preference
        every { userJobGroupFacade.findIdsByUserIdAndStatus(user.id, UserJobStatus.NEW) } returns ids
        every { userJobGroupFacade.findByIdsWithGroupAndJobs(user.id, UserJobStatus.NEW, ids) } returns groups
        every { userJobGroupFacade.flush() } just Runs
        every { entityManager.clear() } just Runs
    }

    private fun group(title: String): JobGroupEntity =
        JobGroupEntity(
            groupKey = JobGroupKeyComputer.compute(title, null),
            title = title,
        )

    private fun groupWithJob(title: String): JobGroupEntity = groupWithJobs(title, job(title))

    private fun groupWithJobs(
        title: String,
        vararg jobs: JobEntity,
    ): JobGroupEntity =
        JobGroupEntity(
            groupKey = JobGroupKeyComputer.compute(title, null),
            title = title,
            jobs = jobs.toList(),
        )

    private fun job(
        title: String,
        description: String = "Looking for a developer with Spring experience",
    ): JobEntity =
        JobEntity(
            title = title,
            group = group(title),
            url = "https://example.com/${title.hashCode()}-${description.hashCode()}",
            description = description,
            source = JobSource.DOU,
            remote = true,
            lastSeenAt = Instant.parse("2026-01-01T00:00:00Z"),
        )

    private fun userJobGroup(
        user: UserEntity,
        group: JobGroupEntity,
    ): UserJobGroupEntity = UserJobGroupEntity(user = user, group = group)

    private fun preference(
        user: UserEntity,
        excludedKeywords: List<String> = emptyList(),
        excludedTitleKeywords: List<String> = emptyList(),
    ): UserPreferenceEntity =
        UserPreferenceEntity(
            user = user,
            search = SearchPreferences(),
            matching =
            MatchingPreferences(
                excludedKeywords = excludedKeywords,
                excludedTitleKeywords = excludedTitleKeywords,
            ),
            telegram = TelegramPreferences(),
        )
}
