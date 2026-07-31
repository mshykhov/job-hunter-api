package com.mshykhov.jobhunter.application.job

import com.mshykhov.jobhunter.application.user.UserRepository
import com.mshykhov.jobhunter.application.userjob.UserJobGroupRepository
import com.mshykhov.jobhunter.application.userjob.UserJobRepository
import com.mshykhov.jobhunter.application.userjob.UserJobStatus
import com.mshykhov.jobhunter.support.AbstractIntegrationTest
import com.mshykhov.jobhunter.support.TestFixtures
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JobFacadeIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var jobFacade: JobFacade

    @Autowired
    lateinit var jobRepository: JobRepository

    @Autowired
    lateinit var jobGroupRepository: JobGroupRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var userJobGroupRepository: UserJobGroupRepository

    @Autowired
    lateinit var userJobRepository: UserJobRepository

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

    @Nested
    inner class FindPurgeableIds {
        private val reference = Instant.parse("2026-07-01T00:00:00Z")
        private val threshold = reference.minus(Duration.ofDays(30))

        @Test
        fun `should keep a job last seen inside the retention window`() {
            val suffix = System.nanoTime()
            val group = jobGroupRepository.save(TestFixtures.jobGroupEntity(title = "Fresh $suffix"))
            val recent =
                jobRepository.save(
                    TestFixtures.jobEntity(title = "Fresh $suffix", group = group, url = "https://example.com/fresh-$suffix")
                        .apply {
                            lastSeenAt = reference.minus(Duration.ofDays(1))
                            matchedAt = reference.minus(Duration.ofDays(1))
                        },
                )

            val result = jobFacade.findPurgeableIds(threshold, 500)

            assertFalse(recent.id in result)
        }

        @Test
        fun `should mark a stale evaluated unengaged job as purgeable`() {
            val suffix = System.nanoTime()
            val group = jobGroupRepository.save(TestFixtures.jobGroupEntity(title = "Stale $suffix"))
            val stale =
                jobRepository.save(
                    TestFixtures.jobEntity(title = "Stale $suffix", group = group, url = "https://example.com/stale-$suffix")
                        .apply {
                            lastSeenAt = reference.minus(Duration.ofDays(40))
                            matchedAt = reference.minus(Duration.ofDays(40))
                        },
                )

            val result = jobFacade.findPurgeableIds(threshold, 500)

            assertTrue(stale.id in result)
        }

        @Test
        fun `should keep a stale job that was never evaluated`() {
            val suffix = System.nanoTime()
            val group = jobGroupRepository.save(TestFixtures.jobGroupEntity(title = "Unevaluated $suffix"))
            val unevaluated =
                jobRepository.save(
                    TestFixtures.jobEntity(
                        title = "Unevaluated $suffix",
                        group = group,
                        url = "https://example.com/unevaluated-$suffix",
                    ).apply {
                        lastSeenAt = reference.minus(Duration.ofDays(400))
                        matchedAt = null
                    },
                )

            val result = jobFacade.findPurgeableIds(threshold, 500)

            assertFalse(unevaluated.id in result)
        }

        @Test
        fun `should keep a stale evaluated job whose group has a user_job_groups row in any status`() {
            val suffix = System.nanoTime()
            val group = jobGroupRepository.save(TestFixtures.jobGroupEntity(title = "Engaged $suffix"))
            val engaged =
                jobRepository.save(
                    TestFixtures.jobEntity(title = "Engaged $suffix", group = group, url = "https://example.com/engaged-$suffix")
                        .apply {
                            lastSeenAt = reference.minus(Duration.ofDays(40))
                            matchedAt = reference.minus(Duration.ofDays(40))
                        },
                )
            val user = userRepository.save(TestFixtures.userEntity())
            userJobGroupRepository.save(
                TestFixtures.userJobGroupEntity(user = user, group = group, status = UserJobStatus.IRRELEVANT),
            )

            val result = jobFacade.findPurgeableIds(threshold, 500)

            assertFalse(engaged.id in result)
        }

        @Test
        fun `should keep a stale evaluated job that has a generated cover letter or recruiter message`() {
            val suffix = System.nanoTime()
            val group = jobGroupRepository.save(TestFixtures.jobGroupEntity(title = "CoverLetter $suffix"))
            val coverLetterJob =
                jobRepository.save(
                    TestFixtures.jobEntity(title = "CoverLetter $suffix", group = group, url = "https://example.com/cover-letter-$suffix")
                        .apply {
                            lastSeenAt = reference.minus(Duration.ofDays(40))
                            matchedAt = reference.minus(Duration.ofDays(40))
                        },
                )
            val user = userRepository.save(TestFixtures.userEntity())
            userJobRepository.save(TestFixtures.userJobEntity(user = user, job = coverLetterJob))

            val result = jobFacade.findPurgeableIds(threshold, 500)

            assertFalse(coverLetterJob.id in result)
        }

        @Test
        fun `should cap the number of purgeable ids returned to the requested batch size and order by staleness`() {
            val suffix = System.nanoTime()
            val jobs =
                (0..2).map { index ->
                    val group = jobGroupRepository.save(TestFixtures.jobGroupEntity(title = "Batch $suffix-$index"))
                    jobRepository.save(
                        TestFixtures.jobEntity(title = "Batch $suffix-$index", group = group, url = "https://example.com/batch-$suffix-$index")
                            .apply {
                                lastSeenAt = Instant.EPOCH.plus(Duration.ofMinutes(index.toLong()))
                                matchedAt = Instant.EPOCH
                            },
                    )
                }

            val result = jobRepository.findPurgeableIds(threshold, PageRequest.of(0, 2))

            assertEquals(2, result.size)
            assertEquals(listOf(jobs[0].id, jobs[1].id), result)
        }
    }
}
