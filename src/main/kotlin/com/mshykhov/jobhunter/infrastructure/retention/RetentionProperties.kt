package com.mshykhov.jobhunter.infrastructure.retention

import jakarta.validation.constraints.Positive
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.convert.DurationUnit
import org.springframework.validation.annotation.Validated
import java.time.Duration
import java.time.temporal.ChronoUnit

@ConfigurationProperties(prefix = "jobhunter.retention")
@Validated
data class RetentionProperties(
    val enabled: Boolean = true,
    @field:DurationUnit(ChronoUnit.DAYS)
    val retentionPeriod: Duration = Duration.ofDays(30),
    @field:DurationUnit(ChronoUnit.HOURS)
    val gracePeriod: Duration = Duration.ofHours(48),
    @field:Positive
    val batchSize: Int = 500,
    @field:Positive
    val maxPerRun: Int = 5000,
)
