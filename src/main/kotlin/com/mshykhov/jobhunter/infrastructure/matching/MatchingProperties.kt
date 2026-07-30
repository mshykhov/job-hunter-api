package com.mshykhov.jobhunter.infrastructure.matching

import jakarta.validation.constraints.Positive
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.Duration

@ConfigurationProperties(prefix = "jobhunter.matching")
@Validated
data class MatchingProperties(
    @field:Positive
    val batchSize: Int = 200,
    @field:Positive
    val maxAttempts: Int = 5,
    val backoffInitial: Duration = Duration.ofMinutes(1),
    val backoffMax: Duration = Duration.ofMinutes(30),
)
