package com.mshykhov.jobhunter.application.job

import com.mshykhov.jobhunter.infrastructure.metrics.MatchingMetrics
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test

class JobRetentionSchedulerTest {
    private val jobRetentionService = mockk<JobRetentionService>()
    private val matchingMetrics = mockk<MatchingMetrics>(relaxed = true)
    private val scheduler = JobRetentionScheduler(jobRetentionService, matchingMetrics)

    @Test
    fun `should record the purge metric only after the service call returns`() {
        every { jobRetentionService.purgeExpiredJobs() } returns PurgeSummary(jobsDeleted = 3, groupsDeleted = 1)

        scheduler.purgeExpiredJobs()

        verify { matchingMetrics.recordPurge(3) }
    }

    @Test
    fun `should not record a metric when nothing was purged`() {
        every { jobRetentionService.purgeExpiredJobs() } returns PurgeSummary(jobsDeleted = 0, groupsDeleted = 0)

        scheduler.purgeExpiredJobs()

        verify(exactly = 0) { matchingMetrics.recordPurge(any()) }
    }

    @Test
    fun `should not record a metric and not propagate when the purge run throws`() {
        every { jobRetentionService.purgeExpiredJobs() } throws RuntimeException("constraint violation")

        assertDoesNotThrow { scheduler.purgeExpiredJobs() }

        verify(exactly = 0) { matchingMetrics.recordPurge(any()) }
    }
}
