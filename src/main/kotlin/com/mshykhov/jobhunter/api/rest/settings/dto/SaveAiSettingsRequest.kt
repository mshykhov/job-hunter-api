package com.mshykhov.jobhunter.api.rest.settings.dto

import com.mshykhov.jobhunter.application.ai.UserAiProviderEntity
import com.mshykhov.jobhunter.application.common.ValidationException
import com.mshykhov.jobhunter.application.settings.AiProvider
import com.mshykhov.jobhunter.application.user.UserEntity
import jakarta.validation.constraints.NotBlank

data class SaveAiSettingsRequest(
    val apiKey: String? = null,
    @field:NotBlank
    val modelId: String,
) {
    fun applyTo(target: UserAiProviderEntity) {
        providedApiKey()?.let { target.apiKey = it }
        target.modelId = modelId
    }

    fun toEntity(user: UserEntity): UserAiProviderEntity =
        UserAiProviderEntity(
            user = user,
            priority = PRIMARY_PRIORITY,
            provider = AiProvider.OPENAI,
            apiKey = providedApiKey() ?: throw ValidationException("apiKey: must not be blank"),
            modelId = modelId,
        )

    private fun providedApiKey(): String? = apiKey?.takeIf { it.isNotBlank() }

    companion object {
        const val PRIMARY_PRIORITY = 1
    }
}
