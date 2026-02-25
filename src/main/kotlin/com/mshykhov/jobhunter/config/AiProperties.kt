package com.mshykhov.jobhunter.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jobhunter.ai")
data class AiProperties(
    val enabled: Boolean = false,
    val apiKey: String = "",
    val authToken: String = "",
    val baseUrl: String = "https://api.anthropic.com",
    val model: String = "claude-haiku-4-5-20251001",
    val filter: FilterProperties = FilterProperties(),
    val normalize: NormalizeProperties = NormalizeProperties(),
) {
    data class FilterProperties(
        val minScore: Int = 50,
        val maxTokens: Long = 512,
    )

    data class NormalizeProperties(
        val maxTokens: Long = 1024,
    )
}
