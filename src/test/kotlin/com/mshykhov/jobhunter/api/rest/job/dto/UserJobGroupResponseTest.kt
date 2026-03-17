package com.mshykhov.jobhunter.api.rest.job.dto

import com.mshykhov.jobhunter.application.job.JobSource
import com.mshykhov.jobhunter.support.TestFixtures
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserJobGroupResponseTest {
    @Nested
    inner class From {
        @Test
        fun `should compute sources from group jobs`() {
            val group = TestFixtures.jobGroupEntity()
            setJobs(
                group,
                listOf(
                    TestFixtures.jobEntity(group = group, source = JobSource.DOU),
                    TestFixtures.jobEntity(group = group, source = JobSource.DJINNI),
                    TestFixtures.jobEntity(group = group, source = JobSource.DOU),
                ),
            )
            val userJobGroup = TestFixtures.userJobGroupEntity(group = group)

            val result = UserJobGroupResponse.from(userJobGroup)

            assertEquals(listOf(JobSource.DJINNI, JobSource.DOU), result.sources)
        }

        @Test
        fun `should compute distinct sorted locations from group jobs`() {
            val group = TestFixtures.jobGroupEntity()
            setJobs(
                group,
                listOf(
                    TestFixtures.jobEntity(group = group, location = "Kyiv"),
                    TestFixtures.jobEntity(group = group, location = "Berlin"),
                    TestFixtures.jobEntity(group = group, location = "Kyiv"),
                    TestFixtures.jobEntity(group = group, location = null),
                ),
            )
            val userJobGroup = TestFixtures.userJobGroupEntity(group = group)

            val result = UserJobGroupResponse.from(userJobGroup)

            assertEquals(listOf("Berlin", "Kyiv"), result.locations)
        }

        @Test
        fun `should pick first non-null salary`() {
            val group = TestFixtures.jobGroupEntity()
            setJobs(
                group,
                listOf(
                    TestFixtures.jobEntity(group = group, salary = null),
                    TestFixtures.jobEntity(group = group, salary = "5000 USD"),
                    TestFixtures.jobEntity(group = group, salary = "6000 EUR"),
                ),
            )
            val userJobGroup = TestFixtures.userJobGroupEntity(group = group)

            val result = UserJobGroupResponse.from(userJobGroup)

            assertEquals("5000 USD", result.salary)
        }

        @Test
        fun `should pick earliest publishedAt`() {
            val early = Instant.parse("2026-01-01T00:00:00Z")
            val late = Instant.parse("2026-03-01T00:00:00Z")
            val group = TestFixtures.jobGroupEntity()
            val job1 = TestFixtures.jobEntity(group = group).also { it.publishedAt = late }
            val job2 = TestFixtures.jobEntity(group = group).also { it.publishedAt = early }
            val job3 = TestFixtures.jobEntity(group = group).also { it.publishedAt = null }
            setJobs(group, listOf(job1, job2, job3))
            val userJobGroup = TestFixtures.userJobGroupEntity(group = group)

            val result = UserJobGroupResponse.from(userJobGroup)

            assertEquals(early, result.publishedAt)
        }

        @Test
        fun `should derive jobCount from actual jobs size`() {
            val group = TestFixtures.jobGroupEntity()
            setJobs(
                group,
                listOf(
                    TestFixtures.jobEntity(group = group),
                    TestFixtures.jobEntity(group = group),
                    TestFixtures.jobEntity(group = group),
                ),
            )
            val userJobGroup = TestFixtures.userJobGroupEntity(group = group)

            val result = UserJobGroupResponse.from(userJobGroup)

            assertEquals(3, result.jobCount)
        }

        @Test
        fun `should return empty sources when group has no jobs`() {
            val group = TestFixtures.jobGroupEntity()
            val userJobGroup = TestFixtures.userJobGroupEntity(group = group)

            val result = UserJobGroupResponse.from(userJobGroup)

            assertEquals(0, result.jobCount)
            assertTrue(result.sources.isEmpty())
            assertTrue(result.locations.isEmpty())
            assertNull(result.salary)
            assertNull(result.publishedAt)
        }

        @Test
        fun `should handle group with single job`() {
            val group = TestFixtures.jobGroupEntity()
            val job =
                TestFixtures
                    .jobEntity(
                        group = group,
                        source = JobSource.LINKEDIN,
                        location = "Remote",
                        salary = "7000 USD",
                    ).also { it.publishedAt = Instant.parse("2026-02-15T12:00:00Z") }
            setJobs(group, listOf(job))
            val userJobGroup = TestFixtures.userJobGroupEntity(group = group)

            val result = UserJobGroupResponse.from(userJobGroup)

            assertEquals(listOf(JobSource.LINKEDIN), result.sources)
            assertEquals(listOf("Remote"), result.locations)
            assertEquals("7000 USD", result.salary)
            assertEquals(Instant.parse("2026-02-15T12:00:00Z"), result.publishedAt)
            assertEquals(group.id, result.groupId)
            assertEquals(group.title, result.title)
            assertEquals(group.company, result.company)
        }

        private fun setJobs(
            group: com.mshykhov.jobhunter.application.job.JobGroupEntity,
            jobs: List<com.mshykhov.jobhunter.application.job.JobEntity>,
        ) {
            val field = group.javaClass.getDeclaredField("jobs")
            field.isAccessible = true
            field.set(group, jobs)
        }
    }
}
