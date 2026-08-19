package com.mshykhov.jobhunter.infrastructure.automation

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "jobhunter.automation")
data class AutomationProperties(
    val enabled: Boolean = false,
    val ownerIssuer: String = "",
    val ownerSubject: String = "",
    val runnerIssuer: String = "",
    val heartbeatFreshness: Duration = Duration.ofMinutes(2),
    val preflightFreshness: Duration = Duration.ofMinutes(10),
    val codexFreshness: Duration = Duration.ofHours(12),
    val maxClockSkew: Duration = Duration.ofMinutes(1),
)
