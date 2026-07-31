package com.mshykhov.jobhunter.api.rest.settings.dto

import com.mshykhov.jobhunter.application.ai.UserAiProviderEntity

data class AiSettingsResponse(val modelId: String, val apiKeyHint: String) {
    companion object {
        fun from(entity: UserAiProviderEntity): AiSettingsResponse =
            AiSettingsResponse(
                modelId = entity.modelId,
                apiKeyHint = maskApiKey(entity.apiKey, entity.provider.requiresApiKey),
            )
    }
}
