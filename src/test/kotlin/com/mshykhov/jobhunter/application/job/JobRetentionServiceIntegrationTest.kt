package com.mshykhov.jobhunter.application.job

import com.mshykhov.jobhunter.application.user.UserRepository
import com.mshykhov.jobhunter.application.userjob.UserJobGroupRepository
import com.mshykhov.jobhunter.application.userjob.UserJobRepository
import com.mshykhov.jobhunter.application.userjob.UserJobStatus
import com.mshykhov.jobhunter.infrastructure.retention.RetentionAnchor
import com.mshykhov.jobhunter.infrastructure.retention.RetentionProperties
import com.mshykhov.jobhunter.support.AbstractIntegrationTest
import com.mshykhov.jobhunter.support.TestFixtures
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JobRetentionServiceIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var jobFacade: JobFacade

    @Autowired
    lateinit var jobGroupFacade: JobGroupFacade

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

    private val now = Instant.parse("1971-01-01T00:00:00Z")
    private val staleInstant = Instant.parse("1970-10-01T00:00:00Z")
    private val freshInstant = Instant.parse("1970-12-20T00:00:00Z")
    private val retentionProperties =
        RetentionProperties(
            enabled = true,
            retentionPeriod = Duration.ofDays(30),
            gracePeriod = Duration.ZERO,
            batchSize = 500,
            maxPerRun = 5000,
        )

    // The real anchor resolves to the V25 migration timestamp, which is later than this
    // test's fixed clock; RetentionAnchorIntegrationTest covers that resolution instead.
    private val retentionAnchor = mockk<RetentionAnchor>().also { every { it.instant() } returns now }

    private fun service() =
        JobRetentionService(jobFacade, jobGroupFacade, retentionProperties, retentionAnchor, Clock.fixed(now, ZoneOffset.UTC))

    @Test
    fun `should purge stale unengaged jobs and their emptied groups while sparing guarded jobs`() {
        val suffix = System.nanoTime()

        val purgeableGroup = jobGroupRepository.save(TestFixtures.jobGroupEntity(title = "Purgeable $suffix"))
        val purgeableJob =
            jobRepository.save(
                TestFixtures.jobEntity(title = "Purgeable $suffix", group = purgeableGroup, url = "https://example.com/purge-$suffix")
                    .apply {
                        lastSeenAt = staleInstant
                        matchedAt = staleInstant
                    },
            )

        val recentGroup = jobGroupRepository.save(TestFixtures.jobGroupEntity(title = "Recent $suffix"))
        val recentJob =
            jobRepository.save(
                TestFixtures.jobEntity(title = "Recent $suffix", group = recentGroup, url = "https://example.com/recent-$suffix")
                    .apply {
                        lastSeenAt = freshInstant
                        matchedAt = freshInstant
                    },
            )

        val engagedGroup = jobGroupRepository.save(TestFixtures.jobGroupEntity(title = "Engaged $suffix"))
        val engagedJob =
            jobRepository.save(
                TestFixtures.jobEntity(title = "Engaged $suffix", group = engagedGroup, url = "https://example.com/engaged-$suffix")
                    .apply {
                        lastSeenAt = staleInstant
                        matchedAt = staleInstant
                    },
            )
        val engagedUser = userRepository.save(TestFixtures.userEntity())
        userJobGroupRepository.save(
            TestFixtures.userJobGroupEntity(user = engagedUser, group = engagedGroup, status = UserJobStatus.IRRELEVANT),
        )

        val sharedGroup = jobGroupRepository.save(TestFixtures.jobGroupEntity(title = "Shared $suffix"))
        val purgeableSharedJob =
            jobRepository.save(
                TestFixtures.jobEntity(title = "Shared $suffix", group = sharedGroup, url = "https://example.com/shared-purge-$suffix")
                    .apply {
                        lastSeenAt = staleInstant
                        matchedAt = staleInstant
                    },
            )
        val survivingSharedJob =
            jobRepository.save(
                TestFixtures.jobEntity(title = "Shared $suffix", group = sharedGroup, url = "https://example.com/shared-keep-$suffix")
                    .apply {
                        lastSeenAt = freshInstant
                        matchedAt = freshInstant
                    },
            )

        val coverLetterGroup = jobGroupRepository.save(TestFixtures.jobGroupEntity(title = "CoverLetter $suffix"))
        val coverLetterJob =
            jobRepository.save(
                TestFixtures.jobEntity(title = "CoverLetter $suffix", group = coverLetterGroup, url = "https://example.com/cover-$suffix")
                    .apply {
                        lastSeenAt = staleInstant
                        matchedAt = staleInstant
                    },
            )
        val coverLetterUser = userRepository.save(TestFixtures.userEntity())
        userJobRepository.save(TestFixtures.userJobEntity(user = coverLetterUser, job = coverLetterJob))

        val summary = service().purgeExpiredJobs()

        assertTrue(summary.jobsDeleted >= 2)
        assertFalse(jobRepository.existsById(purgeableJob.id))
        assertFalse(jobGroupRepository.existsById(purgeableGroup.id))

        assertTrue(jobRepository.existsById(recentJob.id))
        assertTrue(jobGroupRepository.existsById(recentGroup.id))

        assertTrue(jobRepository.existsById(engagedJob.id))
        assertTrue(jobGroupRepository.existsById(engagedGroup.id))

        assertFalse(jobRepository.existsById(purgeableSharedJob.id))
        assertTrue(jobRepository.existsById(survivingSharedJob.id))
        assertTrue(jobGroupRepository.existsById(sharedGroup.id))

        assertTrue(jobRepository.existsById(coverLetterJob.id))
        assertTrue(jobGroupRepository.existsById(coverLetterGroup.id))
    }
}
