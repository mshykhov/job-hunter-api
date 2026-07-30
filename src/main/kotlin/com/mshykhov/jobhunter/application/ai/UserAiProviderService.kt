package com.mshykhov.jobhunter.application.ai

import com.mshykhov.jobhunter.api.rest.settings.dto.AiProviderChainResponse
import com.mshykhov.jobhunter.api.rest.settings.dto.AiSettingsResponse
import com.mshykhov.jobhunter.api.rest.settings.dto.SaveAiProviderChainRequest
import com.mshykhov.jobhunter.api.rest.settings.dto.SaveAiSettingsRequest
import com.mshykhov.jobhunter.application.common.AiNotConfiguredException
import com.mshykhov.jobhunter.application.user.UserFacade
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserAiProviderService(private val userFacade: UserFacade, private val userAiProviderFacade: UserAiProviderFacade) {
    @Transactional(readOnly = true)
    fun get(auth0Sub: String): AiSettingsResponse = AiSettingsResponse.from(findPriorityOneRow(auth0Sub))

    @Transactional
    fun save(
        auth0Sub: String,
        request: SaveAiSettingsRequest,
    ): AiSettingsResponse {
        val user = userFacade.findOrCreate(auth0Sub)
        val existing = chainFor(user.id).find { it.priority == SaveAiSettingsRequest.PRIMARY_PRIORITY }
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
        val entities = request.toEntities(user)
        userAiProviderFacade.deleteAll(user.id)
        return AiProviderChainResponse.from(userAiProviderFacade.saveAll(entities))
    }

    @Transactional(readOnly = true)
    fun resolvePrimary(auth0Sub: String): UserAiProviderEntity {
        val user = userFacade.findByAuth0Sub(auth0Sub) ?: throw AiNotConfiguredException()
        return chainFor(user.id).firstOrNull { it.enabled } ?: throw AiNotConfiguredException()
    }

    @Transactional(readOnly = true)
    fun chainFor(userId: UUID): List<UserAiProviderEntity> = userAiProviderFacade.findByUserId(userId)

    private fun findPriorityOneRow(auth0Sub: String): UserAiProviderEntity {
        val user = userFacade.findByAuth0Sub(auth0Sub) ?: throw AiNotConfiguredException()
        return chainFor(user.id).find { it.priority == SaveAiSettingsRequest.PRIMARY_PRIORITY } ?: throw AiNotConfiguredException()
    }
}
