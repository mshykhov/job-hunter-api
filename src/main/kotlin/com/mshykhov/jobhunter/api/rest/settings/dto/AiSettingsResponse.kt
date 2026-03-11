package com.mshykhov.jobhunter.api.rest.settings.dto

import com.mshykhov.jobhunter.application.ai.UserAiSettingsEntity

data class AiSettingsResponse(val modelId: String, val apiKeyHint: String) {
    companion object {
        fun from(entity: UserAiSettingsEntity): AiSettingsResponse =
            AiSettingsResponse(
                modelId = entity.modelId,
                apiKeyHint = maskApiKey(entity.apiKey),
            )

        private fun maskApiKey(apiKey: String): String =
            if (apiKey.length > 8) {
                "${apiKey.take(8)}${"*".repeat(apiKey.length - 8)}"
            } else {
                "*".repeat(apiKey.length)
            }
    }
}
