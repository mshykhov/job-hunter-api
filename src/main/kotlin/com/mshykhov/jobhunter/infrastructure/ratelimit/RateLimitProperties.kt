package com.mshykhov.jobhunter.infrastructure.ratelimit

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jobhunter.rate-limit")
data class RateLimitProperties(
    val maxRequests: Int = 60,
    val windowSeconds: Long = 60,
    val maxCacheSize: Long = 10_000,
)
