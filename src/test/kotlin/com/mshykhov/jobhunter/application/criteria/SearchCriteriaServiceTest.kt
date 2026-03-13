package com.mshykhov.jobhunter.application.criteria

import com.mshykhov.jobhunter.application.job.Category
import com.mshykhov.jobhunter.application.job.JobSource
import com.mshykhov.jobhunter.application.preference.UserPreferenceFacade
import com.mshykhov.jobhunter.support.TestFixtures
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SearchCriteriaServiceTest {
    private val userPreferenceFacade = mockk<UserPreferenceFacade>()
    private val service = SearchCriteriaService(userPreferenceFacade)

    @Nested
    inner class GetAggregated {
        @Test
        fun `should return empty criteria when no preferences exist`() {
            every { userPreferenceFacade.findBySourceAllowed(JobSource.DOU.name) } returns emptyList()

            val result = service.getAggregated(JobSource.DOU)

            assertTrue(result.categories.isEmpty())
            assertTrue(result.locations.isEmpty())
            assertFalse(result.remoteOnly)
        }

        @Test
        fun `should aggregate categories from all users`() {
            val pref1 = TestFixtures.userPreferenceEntity(categories = setOf(Category("Backend"), Category("DevOps")))
            val pref2 = TestFixtures.userPreferenceEntity(categories = setOf(Category("Frontend"), Category("Backend")))

            every { userPreferenceFacade.findBySourceAllowed(JobSource.DOU.name) } returns listOf(pref1, pref2)

            val result = service.getAggregated(JobSource.DOU)

            assertEquals(3, result.categories.size)
            assertTrue(result.categories.containsAll(setOf(Category("backend"), Category("devops"), Category("frontend"))))
        }

        @Test
        fun `should aggregate locations from all users`() {
            val pref1 = TestFixtures.userPreferenceEntity(locations = listOf("Kyiv", "Remote"))
            val pref2 = TestFixtures.userPreferenceEntity(locations = listOf("Lviv", "Kyiv"))

            every { userPreferenceFacade.findBySourceAllowed(JobSource.DOU.name) } returns listOf(pref1, pref2)

            val result = service.getAggregated(JobSource.DOU)

            assertEquals(3, result.locations.size)
            assertTrue(result.locations.containsAll(listOf("Kyiv", "Remote", "Lviv")))
        }

        @Test
        fun `should deduplicate categories and locations`() {
            val pref1 = TestFixtures.userPreferenceEntity(categories = setOf(Category("Backend")), locations = listOf("Kyiv"))
            val pref2 = TestFixtures.userPreferenceEntity(categories = setOf(Category("Backend")), locations = listOf("Kyiv"))

            every { userPreferenceFacade.findBySourceAllowed(JobSource.DOU.name) } returns listOf(pref1, pref2)

            val result = service.getAggregated(JobSource.DOU)

            assertEquals(1, result.categories.size)
            assertEquals(1, result.locations.size)
        }

        @Test
        fun `should set remoteOnly true when all users want remote`() {
            val pref1 = TestFixtures.userPreferenceEntity(remoteOnly = true)
            val pref2 = TestFixtures.userPreferenceEntity(remoteOnly = true)

            every { userPreferenceFacade.findBySourceAllowed(JobSource.DOU.name) } returns listOf(pref1, pref2)

            val result = service.getAggregated(JobSource.DOU)

            assertTrue(result.remoteOnly)
        }

        @Test
        fun `should set remoteOnly false when at least one user does not want remote`() {
            val pref1 = TestFixtures.userPreferenceEntity(remoteOnly = true)
            val pref2 = TestFixtures.userPreferenceEntity(remoteOnly = false)

            every { userPreferenceFacade.findBySourceAllowed(JobSource.DOU.name) } returns listOf(pref1, pref2)

            val result = service.getAggregated(JobSource.DOU)

            assertFalse(result.remoteOnly)
        }

        @Test
        fun `should set remoteOnly false when preferences list is empty`() {
            every { userPreferenceFacade.findBySourceAllowed(JobSource.DOU.name) } returns emptyList()

            val result = service.getAggregated(JobSource.DOU)

            assertFalse(result.remoteOnly)
        }

        @Test
        fun `should handle single user with preferences`() {
            val pref =
                TestFixtures.userPreferenceEntity(
                    categories = setOf(Category("Backend")),
                    locations = listOf("Kyiv"),
                    remoteOnly = true,
                )

            every { userPreferenceFacade.findBySourceAllowed(JobSource.DJINNI.name) } returns listOf(pref)

            val result = service.getAggregated(JobSource.DJINNI)

            assertEquals(setOf(Category("backend")), result.categories)
            assertEquals(listOf("Kyiv"), result.locations)
            assertTrue(result.remoteOnly)
        }
    }
}
