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

        @Test
        fun `should back off when the matching run throws instead of returning an outcome`() {
            every { jobMatchingService.processUnmatchedJobs() } throws RuntimeException("decryption failed")

            val scheduler = schedulerAt(Instant.parse("2026-07-30T12:00:00Z"))
            scheduler.processUnmatchedJobs()
            scheduler.processUnmatchedJobs()

            verify(exactly = 1) { jobMatchingService.processUnmatchedJobs() }
        }

        @Test
        fun `should double the delay again on the third consecutive failure`() {
            val clock = mockk<Clock>()
            every { clock.instant() } returnsMany
                listOf(
                    Instant.parse("2026-07-30T00:00:00Z"),
                    Instant.parse("2026-07-30T00:01:00Z"),
                    Instant.parse("2026-07-30T00:03:00Z"),
                    Instant.parse("2026-07-30T00:06:59Z"),
                    Instant.parse("2026-07-30T00:07:00Z"),
                )
            every { jobMatchingService.processUnmatchedJobs() } returns MatchingOutcome.AI_UNAVAILABLE
            val scheduler = JobMatchingScheduler(jobMatchingService, matchingProperties, clock)

            repeat(5) { scheduler.processUnmatchedJobs() }

            verify(exactly = 4) { jobMatchingService.processUnmatchedJobs() }
        }

        @Test
        fun `should clamp the delay at backoffMax instead of continuing to double`() {
            val clock = mockk<Clock>()
            every { clock.instant() } returnsMany
                listOf(
                    Instant.parse("2026-07-30T00:00:00Z"),
                    Instant.parse("2026-07-30T00:01:00Z"),
                    Instant.parse("2026-07-30T00:03:00Z"),
                    Instant.parse("2026-07-30T00:07:00Z"),
                    Instant.parse("2026-07-30T00:15:00Z"),
                    Instant.parse("2026-07-30T00:31:00Z"),
                    Instant.parse("2026-07-30T01:00:59Z"),
                    Instant.parse("2026-07-30T01:01:00Z"),
                )
            every { jobMatchingService.processUnmatchedJobs() } returns MatchingOutcome.AI_UNAVAILABLE
            val scheduler = JobMatchingScheduler(jobMatchingService, matchingProperties, clock)

            repeat(8) { scheduler.processUnmatchedJobs() }

            verify(exactly = 7) { jobMatchingService.processUnmatchedJobs() }
        }
    }
}
