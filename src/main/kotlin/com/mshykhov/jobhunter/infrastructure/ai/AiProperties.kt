package com.mshykhov.jobhunter.infrastructure.ai

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jobhunter.ai")
data class AiProperties(
    val filter: FilterProperties = FilterProperties(),
) {
    data class FilterProperties(
        val minScore: Int = 50,
    )
}
