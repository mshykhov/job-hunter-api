package com.mshykhov.jobhunter.application.matching

import com.mshykhov.jobhunter.infrastructure.matching.MatchingProperties
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class JobMatchingSchedulerTest {
    private val jobMatchingService = mockk<JobMatchingService>()
    private val matchingProperties =
        MatchingProperties(backoffInitial = Duration.ofMinutes(1), backoffMax = Duration.ofMinutes(30))

    private fun schedulerAt(instant: Instant) =
        JobMatchingScheduler(
            jobMatchingService = jobMatchingService,
            matchingProperties = matchingProperties,
            clock = Clock.fixed(instant, ZoneOffset.UTC),
        )

    @Nested
    inner class Backoff {
        @Test
        fun `should skip the next run after AI becomes unavailable`() {
            every { jobMatchingService.processUnmatchedJobs() } returns MatchingOutcome.AI_UNAVAILABLE

            val scheduler = schedulerAt(Instant.parse("2026-07-30T12:00:00Z"))
            scheduler.processUnmatchedJobs()
            scheduler.processUnmatchedJobs()

            verify(exactly = 1) { jobMatchingService.processUnmatchedJobs() }
        }

        @Test
        fun `should resume once the backoff window has elapsed`() {
            val clock = mockk<Clock>()
            every { clock.instant() } returnsMany
                listOf(
                    Instant.parse("2026-07-30T12:00:00Z"),
                    Instant.parse("2026-07-30T12:02:00Z"),
                )
            every { jobMatchingService.processUnmatchedJobs() } returns MatchingOutcome.AI_UNAVAILABLE
            val scheduler = JobMatchingScheduler(jobMatchingService, matchingProperties, clock)

            scheduler.processUnmatchedJobs()
            scheduler.processUnmatchedJobs()

            verify(exactly = 2) { jobMatchingService.processUnmatchedJobs() }
        }

        @Test
        fun `should not skip runs while matching completes normally`() {
            every { jobMatchingService.processUnmatchedJobs() } returns MatchingOutcome.COMPLETED

            val scheduler = schedulerAt(Instant.parse("2026-07-30T12:00:00Z"))
            scheduler.processUnmatchedJobs()
            scheduler.processUnmatchedJobs()

            verify(exactly = 2) { jobMatchingService.processUnmatchedJobs() }
        }
    }
}
