package com.mshykhov.jobhunter.api.rest.settings.dto

import com.mshykhov.jobhunter.application.ai.UserAiProviderEntity
import com.mshykhov.jobhunter.application.common.ValidationException
import com.mshykhov.jobhunter.application.settings.AiProvider
import com.mshykhov.jobhunter.application.user.UserEntity
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class SaveAiProviderChainEntryRequest(
    @field:NotNull
    val priority: Int,
    @field:NotNull
    val provider: AiProvider,
    @field:NotBlank
    val modelId: String,
    val apiKey: String? = null,
    val enabled: Boolean = true,
) {
    fun toEntity(
        user: UserEntity,
        storedApiKey: String?,
    ): UserAiProviderEntity =
        UserAiProviderEntity(
            user = user,
            priority = priority,
            provider = provider,
            apiKey = resolveApiKey(storedApiKey),
            modelId = modelId,
            enabled = enabled,
        )

    private fun resolveApiKey(storedApiKey: String?): String =
        apiKey?.takeIf { it.isNotBlank() }
            ?: storedApiKey
            ?: if (provider.requiresApiKey) {
                throw ValidationException("chain: apiKey must not be blank for ${provider.displayName}")
            } else {
                ""
            }
}
