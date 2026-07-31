package com.mshykhov.jobhunter.infrastructure.retention

import jakarta.validation.constraints.Positive
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.Duration

@ConfigurationProperties(prefix = "jobhunter.retention")
@Validated
data class RetentionProperties(
    val enabled: Boolean = true,
    val retentionPeriod: Duration = Duration.ofDays(30),
    val gracePeriod: Duration = Duration.ofHours(48),
    @field:Positive
    val batchSize: Int = 500,
)
