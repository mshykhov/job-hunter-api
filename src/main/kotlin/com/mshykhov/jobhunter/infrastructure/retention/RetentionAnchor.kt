package com.mshykhov.jobhunter.infrastructure.retention

import io.github.oshai.kotlinlogging.KotlinLogging
import org.flywaydb.core.Flyway
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant

private val logger = KotlinLogging.logger {}

// V25 introduced jobs.last_seen_at, so the column only becomes trustworthy once the
// scrapers have re-seen every job at least once after that migration. Anchoring the
// grace period there instead of application start keeps a restart from handing the
// purge a fresh window and postponing it indefinitely.
private const val LAST_SEEN_AT_VERSION = "25"

@Component
class RetentionAnchor(private val flyway: Flyway, clock: Clock) {
    private val startedAt: Instant = Instant.now(clock)
    private val anchor: Instant by lazy { resolve() }

    fun instant(): Instant = anchor

    private fun resolve(): Instant {
        val installedOn =
            flyway.info().applied()
                .firstOrNull { it.version?.version == LAST_SEEN_AT_VERSION }
                ?.installedOn
                ?.toInstant()
        if (installedOn == null) {
            logger.warn { "Retention anchor: V$LAST_SEEN_AT_VERSION is absent from the Flyway history, falling back to application start" }
            return startedAt
        }
        logger.info { "Retention anchor: grace period measured from V$LAST_SEEN_AT_VERSION applied at $installedOn" }
        return installedOn
    }
}
