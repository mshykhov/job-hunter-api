package com.mshykhov.jobhunter.api.rest.settings.dto

import com.mshykhov.jobhunter.application.ai.UserAiSettingsEntity
import com.mshykhov.jobhunter.application.user.UserEntity
import jakarta.validation.constraints.NotBlank

data class SaveAiSettingsRequest(
    @field:NotBlank
    val apiKey: String,
    @field:NotBlank
    val modelId: String,
) {
    fun applyTo(target: UserAiSettingsEntity) {
        target.apiKey = apiKey
        target.modelId = modelId
    }

    fun toEntity(user: UserEntity): UserAiSettingsEntity =
        UserAiSettingsEntity(
            user = user,
            apiKey = apiKey,
            modelId = modelId,
        )
}
