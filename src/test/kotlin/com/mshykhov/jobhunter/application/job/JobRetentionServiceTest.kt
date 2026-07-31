package com.mshykhov.jobhunter.application.job

import com.mshykhov.jobhunter.infrastructure.retention.RetentionProperties
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class JobRetentionServiceTest {
    private val jobFacade = mockk<JobFacade>()
    private val jobGroupFacade = mockk<JobGroupFacade>()
    private val startInstant = Instant.parse("2026-07-01T00:00:00Z")
    private val retentionProperties =
        RetentionProperties(
            enabled = true,
            retentionPeriod = Duration.ofDays(30),
            gracePeriod = Duration.ofHours(48),
            batchSize = 500,
            maxPerRun = 5000,
        )

    private fun serviceAt(
        properties: RetentionProperties,
        vararg instants: Instant,
    ): JobRetentionService {
        val clock = mockk<Clock>()
        every { clock.instant() } returnsMany instants.toList()
        return JobRetentionService(jobFacade, jobGroupFacade, properties, clock)
    }

    private fun serviceAt(vararg instants: Instant) = serviceAt(retentionProperties, *instants)

    @Nested
    inner class GracePeriod {
        @Test
        fun `should delete nothing while the grace period since application start is unexpired`() {
            val service = serviceAt(startInstant, startInstant.plus(Duration.ofHours(47)))

            val result = service.purgeExpiredJobs()

            assertEquals(PurgeSummary(0, 0), result)
            verify(exactly = 0) { jobFacade.findPurgeableIds(any(), any()) }
        }

        @Test
        fun `should purge once the grace period since application start has elapsed`() {
            val purgeableId = UUID.randomUUID()
            val service = serviceAt(startInstant, startInstant.plus(Duration.ofHours(48)))
            every { jobFacade.findPurgeableIds(any(), any()) } returns listOf(purgeableId)
            every { jobFacade.findGroupIdsByIds(listOf(purgeableId)) } returns emptyList()
            every { jobFacade.deleteByIds(listOf(purgeableId)) } returns Unit
            every { jobGroupFacade.findIdsWithNoJobs(emptyList()) } returns emptyList()

            val result = service.purgeExpiredJobs()

            assertEquals(1, result.jobsDeleted)
        }
    }

    @Nested
    inner class Enabled {
        @Test
        fun `should delete nothing when retention is disabled`() {
            val disabledProperties = retentionProperties.copy(enabled = false)
            val clock = Clock.fixed(startInstant.plus(Duration.ofDays(60)), ZoneOffset.UTC)
            val service = JobRetentionService(jobFacade, jobGroupFacade, disabledProperties, clock)

            val result = service.purgeExpiredJobs()

            assertEquals(PurgeSummary(0, 0), result)
            verify(exactly = 0) { jobFacade.findPurgeableIds(any(), any()) }
        }
    }

    @Nested
    inner class Purge {
        @Test
        fun `should query with the threshold derived from the retention period and the configured batch size`() {
            val now = startInstant.plus(Duration.ofDays(60))
            val service = serviceAt(startInstant, now)
            every { jobFacade.findPurgeableIds(any(), any()) } returns emptyList()

            service.purgeExpiredJobs()

            verify { jobFacade.findPurgeableIds(now.minus(retentionProperties.retentionPeriod), retentionProperties.batchSize) }
        }

        @Test
        fun `should return zero and touch nothing else when no jobs are purgeable`() {
            val service = serviceAt(startInstant, startInstant.plus(Duration.ofDays(60)))
            every { jobFacade.findPurgeableIds(any(), any()) } returns emptyList()

            val result = service.purgeExpiredJobs()

            assertEquals(PurgeSummary(0, 0), result)
            verify(exactly = 0) { jobFacade.deleteByIds(any()) }
            verify(exactly = 0) { jobGroupFacade.deleteByIds(any()) }
        }

        @Test
        fun `should delete purgeable jobs and report the total in the returned summary`() {
            val ids = listOf(UUID.randomUUID(), UUID.randomUUID())
            val service = serviceAt(startInstant, startInstant.plus(Duration.ofDays(60)))
            every { jobFacade.findPurgeableIds(any(), any()) } returns ids
            every { jobFacade.findGroupIdsByIds(ids) } returns emptyList()
            every { jobFacade.deleteByIds(ids) } returns Unit
            every { jobGroupFacade.findIdsWithNoJobs(emptyList()) } returns emptyList()

            val result = service.purgeExpiredJobs()

            assertEquals(2, result.jobsDeleted)
            verify { jobFacade.deleteByIds(ids) }
        }

        @Test
        fun `should delete only the groups reported as now empty, not every group touched by the batch`() {
            val jobIds = listOf(UUID.randomUUID())
            val groupIds = listOf(UUID.randomUUID(), UUID.randomUUID())
            val emptyGroupId = groupIds[0]
            val service = serviceAt(startInstant, startInstant.plus(Duration.ofDays(60)))
            every { jobFacade.findPurgeableIds(any(), any()) } returns jobIds
            every { jobFacade.findGroupIdsByIds(jobIds) } returns groupIds
            every { jobFacade.deleteByIds(jobIds) } returns Unit
            every { jobGroupFacade.findIdsWithNoJobs(groupIds) } returns listOf(emptyGroupId)
            every { jobGroupFacade.deleteByIds(listOf(emptyGroupId)) } returns Unit

            val result = service.purgeExpiredJobs()

            assertEquals(PurgeSummary(1, 1), result)
            verify { jobGroupFacade.deleteByIds(listOf(emptyGroupId)) }
            verify(exactly = 0) { jobGroupFacade.deleteByIds(groupIds) }
        }

        @Test
        fun `should not call group deletion when no groups became empty`() {
            val jobIds = listOf(UUID.randomUUID())
            val groupIds = listOf(UUID.randomUUID())
            val service = serviceAt(startInstant, startInstant.plus(Duration.ofDays(60)))
            every { jobFacade.findPurgeableIds(any(), any()) } returns jobIds
            every { jobFacade.findGroupIdsByIds(jobIds) } returns groupIds
            every { jobFacade.deleteByIds(jobIds) } returns Unit
            every { jobGroupFacade.findIdsWithNoJobs(groupIds) } returns emptyList()

            service.purgeExpiredJobs()

            verify(exactly = 0) { jobGroupFacade.deleteByIds(any()) }
        }
    }

    @Nested
    inner class BatchLooping {
        private val batchedProperties = retentionProperties.copy(batchSize = 2, maxPerRun = 10)

        @Test
        fun `should keep querying further batches until a short batch comes back`() {
            val firstBatch = listOf(UUID.randomUUID(), UUID.randomUUID())
            val secondBatch = listOf(UUID.randomUUID(), UUID.randomUUID())
            val thirdBatch = listOf(UUID.randomUUID())
            val service = serviceAt(batchedProperties, startInstant, startInstant.plus(Duration.ofDays(60)))
            every { jobFacade.findPurgeableIds(any(), 2) } returnsMany listOf(firstBatch, secondBatch, thirdBatch)
            every { jobFacade.findGroupIdsByIds(any()) } returns emptyList()
            every { jobFacade.deleteByIds(any()) } returns Unit
            every { jobGroupFacade.findIdsWithNoJobs(any()) } returns emptyList()

            val result = service.purgeExpiredJobs()

            assertEquals(5, result.jobsDeleted)
            verify(exactly = 3) { jobFacade.findPurgeableIds(any(), 2) }
        }

        @Test
        fun `should stop at the per-run cap even when batches keep coming back full`() {
            val service = serviceAt(batchedProperties, startInstant, startInstant.plus(Duration.ofDays(60)))
            every { jobFacade.findPurgeableIds(any(), 2) } answers { listOf(UUID.randomUUID(), UUID.randomUUID()) }
            every { jobFacade.findGroupIdsByIds(any()) } returns emptyList()
            every { jobFacade.deleteByIds(any()) } returns Unit
            every { jobGroupFacade.findIdsWithNoJobs(any()) } returns emptyList()

            val result = service.purgeExpiredJobs()

            assertEquals(batchedProperties.maxPerRun, result.jobsDeleted)
            verify(exactly = batchedProperties.maxPerRun / batchedProperties.batchSize) { jobFacade.findPurgeableIds(any(), 2) }
        }

        @Test
        fun `should request a shorter final batch so the cap is never exceeded`() {
            val cappedProperties = retentionProperties.copy(batchSize = 2, maxPerRun = 5)
            val service = serviceAt(cappedProperties, startInstant, startInstant.plus(Duration.ofDays(60)))
            every { jobFacade.findPurgeableIds(any(), 2) } answers { listOf(UUID.randomUUID(), UUID.randomUUID()) }
            every { jobFacade.findPurgeableIds(any(), 1) } answers { listOf(UUID.randomUUID()) }
            every { jobFacade.findGroupIdsByIds(any()) } returns emptyList()
            every { jobFacade.deleteByIds(any()) } returns Unit
            every { jobGroupFacade.findIdsWithNoJobs(any()) } returns emptyList()

            val result = service.purgeExpiredJobs()

            assertEquals(5, result.jobsDeleted)
            verify(exactly = 1) { jobFacade.findPurgeableIds(any(), 1) }
        }
    }
}
