package com.mshykhov.jobhunter.application.matching

import com.mshykhov.jobhunter.infrastructure.matching.MatchingProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger {}

@Component
class JobMatchingScheduler(private val jobMatchingService: JobMatchingService, private val matchingProperties: MatchingProperties, private val clock: Clock) {
    private var skipUntil: Instant = Instant.EPOCH
    private var consecutiveFailures: Int = 0

    @Scheduled(fixedDelayString = "\${jobhunter.matching.interval-ms:60000}")
    fun processUnmatchedJobs() {
        val now = Instant.now(clock)
        if (now.isBefore(skipUntil)) return

        when (jobMatchingService.processUnmatchedJobs()) {
            MatchingOutcome.AI_UNAVAILABLE -> {
                consecutiveFailures++
                val delay = backoffDelay(consecutiveFailures)
                skipUntil = now.plus(delay)
                logger.warn {
                    "AI unavailable, pausing matching for ${delay.toSeconds()}s (consecutive failures: $consecutiveFailures)"
                }
            }

            MatchingOutcome.IDLE, MatchingOutcome.COMPLETED -> {
                consecutiveFailures = 0
                skipUntil = Instant.EPOCH
            }
        }
    }

    private fun backoffDelay(attempt: Int): Duration {
        val multiplier = 1L shl (attempt - 1).coerceIn(0, MAX_BACKOFF_SHIFT)
        val candidate = matchingProperties.backoffInitial.multipliedBy(multiplier)
        return if (candidate > matchingProperties.backoffMax) matchingProperties.backoffMax else candidate
    }

    private companion object {
        const val MAX_BACKOFF_SHIFT = 10
    }
}
