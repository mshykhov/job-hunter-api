package com.mshykhov.jobhunter.infrastructure.fingerprint

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jobhunter.scrapeops")
data class ScrapeOpsProperties(
    val enabled: Boolean = false,
    val apiKey: String = "",
    val baseUrl: String = "https://headers.scrapeops.io/v1",
    val numResults: Int = 50,
    val refreshIntervalMinutes: Long = 360,
)
