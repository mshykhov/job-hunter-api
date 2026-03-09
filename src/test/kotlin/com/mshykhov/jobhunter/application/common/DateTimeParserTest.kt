package com.mshykhov.jobhunter.application.common

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DateTimeParserTest {
    @Nested
    inner class NullAndBlankInput {
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = ["   ", "\t", "\n"])
        fun `should return null for null, empty, or blank input`(input: String?) {
            assertNull(DateTimeParser.toInstant(input))
        }
    }

    @Nested
    inner class IsoInstantFormat {
        @Test
        fun `should parse ISO-8601 instant`() {
            val result = DateTimeParser.toInstant("2026-03-01T10:00:00Z")
            assertEquals(Instant.parse("2026-03-01T10:00:00Z"), result)
        }

        @Test
        fun `should parse ISO-8601 with milliseconds`() {
            val result = DateTimeParser.toInstant("2026-03-01T10:00:00.123Z")
            assertEquals(Instant.parse("2026-03-01T10:00:00.123Z"), result)
        }
    }

    @Nested
    inner class SpaceSeparatedWithOffset {
        @Test
        fun `should parse web3career datePosted format with UTC offset`() {
            val result = DateTimeParser.toInstant("2026-03-03 18:48:47 +0000")
            assertEquals(Instant.parse("2026-03-03T18:48:47Z"), result)
        }

        @Test
        fun `should parse with positive timezone offset`() {
            val result = DateTimeParser.toInstant("2026-03-03 18:48:47 +0800")
            assertEquals(Instant.parse("2026-03-03T10:48:47Z"), result)
        }

        @Test
        fun `should parse with negative timezone offset`() {
            val result = DateTimeParser.toInstant("2026-03-03 18:48:47 -0500")
            assertEquals(Instant.parse("2026-03-03T23:48:47Z"), result)
        }

        @Test
        fun `should parse midnight with offset`() {
            val result = DateTimeParser.toInstant("2026-01-01 00:00:00 +0000")
            assertEquals(Instant.parse("2026-01-01T00:00:00Z"), result)
        }

        @Test
        fun `should parse end of day with offset`() {
            val result = DateTimeParser.toInstant("2026-12-31 23:59:59 +0000")
            assertEquals(Instant.parse("2026-12-31T23:59:59Z"), result)
        }
    }

    @Nested
    inner class LocalDateTimeFormat {
        @Test
        fun `should parse ISO local datetime as UTC`() {
            val result = DateTimeParser.toInstant("2026-03-01T10:00:00")
            assertEquals(Instant.parse("2026-03-01T10:00:00Z"), result)
        }
    }

    @Nested
    inner class LocalDateFormat {
        @Test
        fun `should parse ISO local date as start of day UTC`() {
            val result = DateTimeParser.toInstant("2026-03-01")
            assertEquals(Instant.parse("2026-03-01T00:00:00Z"), result)
        }
    }

    @Nested
    inner class UnparsableInput {
        @ParameterizedTest
        @ValueSource(
            strings = [
                "not a date",
                "2026/03/01",
                "March 1, 2026",
                "01-03-2026",
                "2026-13-01T00:00:00Z",
                "2026-03-32T00:00:00Z",
            ],
        )
        fun `should return null for unrecognized formats`(input: String) {
            assertNull(DateTimeParser.toInstant(input))
        }
    }

    @Nested
    inner class ParserPrecedence {
        @Test
        fun `should not confuse ISO instant with space-separated format`() {
            val isoResult = DateTimeParser.toInstant("2026-03-03T18:48:47Z")
            val spaceResult = DateTimeParser.toInstant("2026-03-03 18:48:47 +0000")
            assertEquals(isoResult, spaceResult)
        }
    }
}
