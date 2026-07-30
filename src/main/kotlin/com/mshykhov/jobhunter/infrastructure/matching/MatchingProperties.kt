package com.mshykhov.jobhunter.infrastructure.matching

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "jobhunter.matching")
data class MatchingProperties(
    val batchSize: Int = 200,
    val maxAttempts: Int = 5,
    val backoffInitial: Duration = Duration.ofMinutes(1),
    val backoffMax: Duration = Duration.ofMinutes(30),
)
