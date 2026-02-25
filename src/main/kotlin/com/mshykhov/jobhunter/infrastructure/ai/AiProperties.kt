package com.mshykhov.jobhunter.infrastructure.ai

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jobhunter.ai")
data class AiProperties(
    val matching: MatchingProperties = MatchingProperties(),
) {
    data class MatchingProperties(
        val batchSize: Int = 10,
    )
}
