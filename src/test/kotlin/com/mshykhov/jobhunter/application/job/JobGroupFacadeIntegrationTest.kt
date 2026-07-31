package com.mshykhov.jobhunter.application.job

import com.mshykhov.jobhunter.support.AbstractIntegrationTest
import com.mshykhov.jobhunter.support.TestFixtures
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JobGroupFacadeIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var jobGroupFacade: JobGroupFacade

    @Autowired
    lateinit var jobGroupRepository: JobGroupRepository

    @Autowired
    lateinit var jobRepository: JobRepository

    @Nested
    inner class FindIdsWithNoJobs {
        @Test
        fun `should find a group left with no jobs`() {
            val suffix = System.nanoTime()
            val emptyGroup = jobGroupRepository.save(TestFixtures.jobGroupEntity(title = "Emptied $suffix"))

            val result = jobGroupFacade.findIdsWithNoJobs(listOf(emptyGroup.id))

            assertTrue(emptyGroup.id in result)
        }

        @Test
        fun `should not find a group that still holds a job`() {
            val suffix = System.nanoTime()
            val occupiedGroup = jobGroupRepository.save(TestFixtures.jobGroupEntity(title = "Occupied $suffix"))
            jobRepository.save(
                TestFixtures.jobEntity(title = "Occupied $suffix", group = occupiedGroup, url = "https://example.com/occupied-$suffix"),
            )

            val result = jobGroupFacade.findIdsWithNoJobs(listOf(occupiedGroup.id))

            assertFalse(occupiedGroup.id in result)
        }
    }
}
