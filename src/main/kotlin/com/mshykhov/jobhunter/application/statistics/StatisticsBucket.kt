package com.mshykhov.jobhunter.application.statistics

import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

enum class StatisticsBucket {
    DAY,
    WEEK,
    MONTH,
    ;

    fun startOf(instant: Instant): Instant {
        val dateTime = instant.atZone(ZoneOffset.UTC)
        return when (this) {
            DAY -> dateTime.truncatedTo(ChronoUnit.DAYS).toInstant()
            WEEK -> dateTime.toLocalDate().minusDays((dateTime.dayOfWeek.value - 1).toLong()).atStartOfDay(ZoneOffset.UTC).toInstant()
            MONTH -> dateTime.withDayOfMonth(1).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant()
        }
    }

    fun next(start: Instant): Instant = when (this) {
        DAY -> start.plus(1, ChronoUnit.DAYS)
        WEEK -> start.plus(7, ChronoUnit.DAYS)
        MONTH -> start.atZone(ZoneOffset.UTC).plusMonths(1).toInstant()
    }

    fun postgresUnit(): String = name.lowercase()
}
