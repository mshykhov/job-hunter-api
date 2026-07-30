package com.mshykhov.jobhunter.infrastructure.ai

import com.mshykhov.jobhunter.application.settings.AiProvider
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jobhunter.ai.providers")
data class AiProviderProperties(val codexBaseUrl: String = "", val geminiBaseUrl: String = "") {
    fun baseUrlFor(provider: AiProvider): String? =
        when (provider) {
            AiProvider.CODEX -> codexBaseUrl.takeIf { it.isNotBlank() }
            AiProvider.GEMINI -> geminiBaseUrl.takeIf { it.isNotBlank() }
            AiProvider.OPENAI -> null
        }
}
