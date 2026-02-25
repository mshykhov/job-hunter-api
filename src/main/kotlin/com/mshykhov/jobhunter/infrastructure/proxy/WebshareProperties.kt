package com.mshykhov.jobhunter.infrastructure.proxy

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jobhunter.proxy")
data class WebshareProperties(
    val enabled: Boolean = false,
    val apiKey: String = "",
    val baseUrl: String = "https://proxy.webshare.io/api/v2",
    val cacheTtlMinutes: Long = 60,
)
