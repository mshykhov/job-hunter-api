package com.mshykhov.jobhunter.application.settings

import com.mshykhov.jobhunter.api.rest.settings.dto.AiModelResponse
import com.mshykhov.jobhunter.api.rest.settings.dto.AiProviderResponse
import com.mshykhov.jobhunter.api.rest.settings.dto.AiProvidersResponse
import org.springframework.stereotype.Service

@Service
class SettingsService {
    fun getAiProviders(): AiProvidersResponse {
        val providers =
            AiModel.entries
                .groupBy { it.provider }
                .map { (provider, models) ->
                    AiProviderResponse(
                        id = provider.id,
                        name = provider.displayName,
                        recommended = provider.recommended,
                        models = models.map { AiModelResponse.from(it) },
                    )
                }
        return AiProvidersResponse(providers = providers)
    }
}
