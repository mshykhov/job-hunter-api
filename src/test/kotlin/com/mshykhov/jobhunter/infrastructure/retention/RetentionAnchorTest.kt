package com.mshykhov.jobhunter.infrastructure.retention

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.MigrationInfo
import org.flywaydb.core.api.MigrationInfoService
import org.flywaydb.core.api.MigrationVersion
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Date
import kotlin.test.assertEquals

class RetentionAnchorTest {
    private val startInstant = Instant.parse("2026-07-31T00:00:00Z")
    private val migratedInstant = Instant.parse("2026-06-01T09:15:00Z")
    private val clock = Clock.fixed(startInstant, ZoneOffset.UTC)

    private fun migration(
        version: String,
        installedOn: Instant?,
    ): MigrationInfo {
        val info = mockk<MigrationInfo>()
        every { info.version } returns MigrationVersion.fromVersion(version)
        every { info.installedOn } returns installedOn?.let(Date::from)
        return info
    }

    private fun flywayWith(vararg applied: MigrationInfo): Flyway {
        val infoService = mockk<MigrationInfoService>()
        every { infoService.applied() } returns arrayOf(*applied)
        val flyway = mockk<Flyway>()
        every { flyway.info() } returns infoService
        return flyway
    }

    @Test
    fun `should anchor on the timestamp of the last-seen-at migration`() {
        val flyway = flywayWith(migration("24", startInstant), migration("25", migratedInstant))

        val anchor = RetentionAnchor(flyway, clock)

        assertEquals(migratedInstant, anchor.instant())
    }

    @Test
    fun `should fall back to application start when the last-seen-at migration is absent from the history`() {
        val flyway = flywayWith(migration("24", migratedInstant))

        val anchor = RetentionAnchor(flyway, clock)

        assertEquals(startInstant, anchor.instant())
    }

    @Test
    fun `should read the migration history only once`() {
        val flyway = flywayWith(migration("25", migratedInstant))
        val anchor = RetentionAnchor(flyway, clock)

        repeat(3) { anchor.instant() }

        verify(exactly = 1) { flyway.info() }
    }
}
