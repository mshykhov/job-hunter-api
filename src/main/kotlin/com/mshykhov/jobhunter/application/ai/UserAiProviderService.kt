package com.mshykhov.jobhunter.application.ai

import com.mshykhov.jobhunter.api.rest.settings.dto.AiProviderChainResponse
import com.mshykhov.jobhunter.api.rest.settings.dto.AiSettingsResponse
import com.mshykhov.jobhunter.api.rest.settings.dto.SaveAiProviderChainRequest
import com.mshykhov.jobhunter.api.rest.settings.dto.SaveAiSettingsRequest
import com.mshykhov.jobhunter.application.common.AiNotConfiguredException
import com.mshykhov.jobhunter.application.common.ValidationException
import com.mshykhov.jobhunter.application.settings.AiModel
import com.mshykhov.jobhunter.application.settings.AiProvider
import com.mshykhov.jobhunter.application.user.UserFacade
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserAiProviderService(private val userFacade: UserFacade, private val userAiProviderFacade: UserAiProviderFacade) {
    @Transactional(readOnly = true)
    fun get(auth0Sub: String): AiSettingsResponse = AiSettingsResponse.from(primaryRow(auth0Sub))

    @Transactional
    fun save(
        auth0Sub: String,
        request: SaveAiSettingsRequest,
    ): AiSettingsResponse {
        val user = userFacade.findOrCreate(auth0Sub)
        val existing = chainFor(user.id).firstOrNull { it.enabled }
        val targetProvider = existing?.provider ?: AiProvider.OPENAI
        validateModelBelongsTo(request.modelId, targetProvider)
        val entity = existing?.also { request.applyTo(it) } ?: request.toEntity(user)
        return AiSettingsResponse.from(userAiProviderFacade.saveAll(listOf(entity)).first())
    }

    @Transactional(readOnly = true)
    fun getChain(auth0Sub: String): AiProviderChainResponse {
        val user = userFacade.findByAuth0Sub(auth0Sub) ?: return AiProviderChainResponse(emptyList())
        return AiProviderChainResponse.from(chainFor(user.id))
    }

    @Transactional
    fun replaceChain(
        auth0Sub: String,
        request: SaveAiProviderChainRequest,
    ): AiProviderChainResponse {
        val user = userFacade.findOrCreate(auth0Sub)
        val storedApiKeys = chainFor(user.id).associate { it.provider to it.apiKey }
        val entities = request.toEntities(user, storedApiKeys)
        userAiProviderFacade.deleteAll(user.id)
        return AiProviderChainResponse.from(userAiProviderFacade.saveAll(entities))
    }

    @Transactional(readOnly = true)
    fun resolvePrimary(auth0Sub: String): UserAiProviderEntity = primaryRow(auth0Sub)

    @Transactional(readOnly = true)
    fun chainFor(userId: UUID): List<UserAiProviderEntity> = userAiProviderFacade.findByUserId(userId)

    private fun primaryRow(auth0Sub: String): UserAiProviderEntity {
        val user = userFacade.findByAuth0Sub(auth0Sub) ?: throw AiNotConfiguredException()
        return chainFor(user.id).firstOrNull { it.enabled } ?: throw AiNotConfiguredException()
    }

    private fun validateModelBelongsTo(
        modelId: String,
        provider: AiProvider,
    ) {
        val model = AiModel.entries.find { it.id == modelId } ?: return
        if (model.provider != provider) {
            throw ValidationException(
                "modelId: ${model.displayName} belongs to ${model.provider.displayName}, not ${provider.displayName}",
            )
        }
    }
}
