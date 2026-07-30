package com.mshykhov.jobhunter.application.job

import com.mshykhov.jobhunter.support.AbstractIntegrationTest
import com.mshykhov.jobhunter.support.TestFixtures
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JobFacadeIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var jobFacade: JobFacade

    @Autowired
    lateinit var jobRepository: JobRepository

    @Autowired
    lateinit var jobGroupRepository: JobGroupRepository

    @Nested
    inner class FindUnmatched {
        @Test
        fun `should exclude jobs that already reached the match attempts cap`() {
            val suffix = System.nanoTime()
            val group = jobGroupRepository.save(TestFixtures.jobGroupEntity(title = "Attempts Cap $suffix"))
            val cappedJob =
                jobRepository.save(
                    TestFixtures.jobEntity(
                        title = "Attempts Cap $suffix",
                        group = group,
                        url = "https://example.com/attempts-cap-$suffix",
                    ).apply { matchAttempts = 5 },
                )
            val eligibleJob =
                jobRepository.save(
                    TestFixtures.jobEntity(
                        title = "Attempts Cap $suffix",
                        group = group,
                        url = "https://example.com/attempts-eligible-$suffix",
                    ),
                )

            val result = jobFacade.findUnmatched(limit = 500, maxAttempts = 5)
            val resultIds = result.map { it.id }

            assertTrue(eligibleJob.id in resultIds)
            assertFalse(cappedJob.id in resultIds)
        }
    }
}
