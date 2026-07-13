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
import com.mshykhov.jobhunter.application.user.UserEntity
import com.mshykhov.jobhunter.application.userjob.UserJobGroupEntity
import com.mshykhov.jobhunter.application.userjob.UserJobGroupFacade
import com.mshykhov.jobhunter.application.userjob.UserJobStatus
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ColdFilterRetroServiceTest {
    private val userPreferenceFacade = mockk<UserPreferenceFacade>()
    private val userJobGroupFacade = mockk<UserJobGroupFacade>()

    private val service = ColdFilterRetroService(userPreferenceFacade, userJobGroupFacade)

    @Test
    fun `should delete NEW groups that fail cold filter after preference change`() {
        val user = UserEntity(auth0Sub = "user-1")
        val leadUserGroup = userJobGroup(user, groupWithJob("Java Team Lead"))
        val devUserGroup = userJobGroup(user, groupWithJob("Senior Java Developer"))
        val preference = preference(user, excludedTitleKeywords = listOf("lead"))
        val deletedSlot = slot<List<UserJobGroupEntity>>()

        every { userPreferenceFacade.findByUserId(user.id) } returns preference
        every { userJobGroupFacade.findByUserIdAndStatus(user.id, UserJobStatus.NEW) } returns
            listOf(leadUserGroup, devUserGroup)
        every { userJobGroupFacade.deleteAll(capture(deletedSlot)) } just Runs

        service.onPreferenceChanged(PreferenceChangedEvent(user.id))

        assertEquals(listOf(leadUserGroup), deletedSlot.captured)
    }

    @Test
    fun `should not delete anything when all groups still pass the filter`() {
        val user = UserEntity(auth0Sub = "user-1")
        val devUserGroup = userJobGroup(user, groupWithJob("Senior Java Developer"))
        val preference = preference(user, excludedTitleKeywords = listOf("lead"))

        every { userPreferenceFacade.findByUserId(user.id) } returns preference
        every { userJobGroupFacade.findByUserIdAndStatus(user.id, UserJobStatus.NEW) } returns listOf(devUserGroup)

        service.onPreferenceChanged(PreferenceChangedEvent(user.id))

        verify(exactly = 0) { userJobGroupFacade.deleteAll(any()) }
    }

    @Test
    fun `should do nothing when user has no preference`() {
        val user = UserEntity(auth0Sub = "user-1")

        every { userPreferenceFacade.findByUserId(user.id) } returns null

        service.onPreferenceChanged(PreferenceChangedEvent(user.id))

        verify(exactly = 0) { userJobGroupFacade.findByUserIdAndStatus(any(), any()) }
    }

    @Test
    fun `should evaluate filter against representative job with longest description`() {
        val user = UserEntity(auth0Sub = "user-1")
        val group = groupWithJobs(
            "Senior Java Developer",
            job("Senior Java Developer", description = "short crypto mention"),
            job("Senior Java Developer", description = "much longer clean description about java and spring boot"),
        )
        val userGroup = userJobGroup(user, group)
        val preference = preference(user, excludedKeywords = listOf("crypto"))

        every { userPreferenceFacade.findByUserId(user.id) } returns preference
        every { userJobGroupFacade.findByUserIdAndStatus(user.id, UserJobStatus.NEW) } returns listOf(userGroup)

        service.onPreferenceChanged(PreferenceChangedEvent(user.id))

        verify(exactly = 0) { userJobGroupFacade.deleteAll(any()) }
    }

    @Test
    fun `should keep group without jobs`() {
        val user = UserEntity(auth0Sub = "user-1")
        val emptyGroup = group("Senior Java Developer")
        val userGroup = userJobGroup(user, emptyGroup)
        val preference = preference(user, excludedTitleKeywords = listOf("lead"))

        every { userPreferenceFacade.findByUserId(user.id) } returns preference
        every { userJobGroupFacade.findByUserIdAndStatus(user.id, UserJobStatus.NEW) } returns listOf(userGroup)

        service.onPreferenceChanged(PreferenceChangedEvent(user.id))

        verify(exactly = 0) { userJobGroupFacade.deleteAll(any()) }
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
