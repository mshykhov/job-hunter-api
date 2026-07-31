package com.mshykhov.jobhunter.api.rest.settings.dto

import com.mshykhov.jobhunter.application.ai.UserAiProviderEntity
import com.mshykhov.jobhunter.application.settings.AiProvider

data class AiProviderChainEntryResponse(val priority: Int, val provider: AiProvider, val modelId: String, val apiKeyHint: String, val enabled: Boolean) {
    companion object {
        fun from(entity: UserAiProviderEntity): AiProviderChainEntryResponse =
            AiProviderChainEntryResponse(
                priority = entity.priority,
                provider = entity.provider,
                modelId = entity.modelId,
                apiKeyHint = maskApiKey(entity.apiKey, entity.provider.requiresApiKey),
                enabled = entity.enabled,
            )
    }
}
