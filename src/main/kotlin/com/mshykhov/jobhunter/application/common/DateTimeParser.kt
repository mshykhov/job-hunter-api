package com.mshykhov.jobhunter.application.common

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object DateTimeParser {
    private val INSTANT_PARSERS: List<(String) -> Instant?> =
        listOf(
            { raw -> tryParse { Instant.parse(raw) } },
            { raw -> tryParse { DateTimeFormatter.RFC_1123_DATE_TIME.parse(raw, Instant::from) } },
            { raw -> tryParse { LocalDateTime.parse(raw).toInstant(ZoneOffset.UTC) } },
        )

    fun toInstant(raw: String?): Instant? {
        if (raw.isNullOrBlank()) return null
        return INSTANT_PARSERS.firstNotNullOfOrNull { it(raw) }
    }

    private inline fun tryParse(block: () -> Instant): Instant? =
        try {
            block()
        } catch (_: DateTimeParseException) {
            null
        }
}
