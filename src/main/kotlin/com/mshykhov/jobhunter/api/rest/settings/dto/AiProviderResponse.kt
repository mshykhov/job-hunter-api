package com.mshykhov.jobhunter.api.rest.settings.dto

import com.mshykhov.jobhunter.application.settings.AiProvider

data class AiProviderResponse(val id: String, val name: String, val recommended: Boolean, val requiresApiKey: Boolean, val models: List<AiModelResponse>) {
    companion object {
        fun from(
            provider: AiProvider,
            models: List<AiModelResponse>,
        ): AiProviderResponse =
            AiProviderResponse(
                id = provider.value,
                name = provider.displayName,
                recommended = provider.recommended,
                requiresApiKey = provider.requiresApiKey,
                models = models,
            )
    }
}
