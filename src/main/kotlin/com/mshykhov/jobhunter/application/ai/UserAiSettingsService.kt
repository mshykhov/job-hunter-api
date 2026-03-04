package com.mshykhov.jobhunter.application.ai

import com.mshykhov.jobhunter.api.rest.settings.dto.AiSettingsResponse
import com.mshykhov.jobhunter.api.rest.settings.dto.SaveAiSettingsRequest
import com.mshykhov.jobhunter.application.common.AiNotConfiguredException
import com.mshykhov.jobhunter.application.user.UserFacade
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserAiSettingsService(
    private val userFacade: UserFacade,
    private val userAiSettingsFacade: UserAiSettingsFacade,
) {
    @Transactional(readOnly = true)
    fun get(auth0Sub: String): AiSettingsResponse {
        val settings = resolveForUser(auth0Sub)
        return AiSettingsResponse.from(settings)
    }

    @Transactional
    fun save(
        auth0Sub: String,
        request: SaveAiSettingsRequest,
    ): AiSettingsResponse {
        val user = userFacade.findOrCreate(auth0Sub)
        val existing = userAiSettingsFacade.findByUserId(user.id)

        val entity =
            if (existing != null) {
                existing.apiKey = request.apiKey
                existing.modelId = request.modelId
                existing
            } else {
                UserAiSettingsEntity(
                    user = user,
                    apiKey = request.apiKey,
                    modelId = request.modelId,
                )
            }

        return AiSettingsResponse.from(userAiSettingsFacade.save(entity))
    }

    @Transactional(readOnly = true)
    fun resolveForUser(auth0Sub: String): UserAiSettingsEntity {
        val user =
            userFacade.findByAuth0Sub(auth0Sub)
                ?: throw AiNotConfiguredException()
        return userAiSettingsFacade.findByUserId(user.id)
            ?: throw AiNotConfiguredException()
    }
}
