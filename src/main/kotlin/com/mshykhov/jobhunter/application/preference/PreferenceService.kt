package com.mshykhov.jobhunter.application.preference

import com.mshykhov.jobhunter.api.rest.exception.custom.NotFoundException
import com.mshykhov.jobhunter.api.rest.preference.dto.PreferenceResponse
import com.mshykhov.jobhunter.api.rest.preference.dto.SavePreferenceRequest
import com.mshykhov.jobhunter.application.ai.PreferenceNormalizer
import com.mshykhov.jobhunter.application.user.UserEntity
import com.mshykhov.jobhunter.application.user.UserFacade
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PreferenceService(
    private val userFacade: UserFacade,
    private val userPreferenceFacade: UserPreferenceFacade,
    private val preferenceNormalizer: PreferenceNormalizer,
) {
    @Transactional(readOnly = true)
    fun get(auth0Sub: String): PreferenceResponse {
        val user =
            userFacade.findByAuth0Sub(auth0Sub)
                ?: throw NotFoundException("Preferences not found")
        val preference =
            userPreferenceFacade.findByUserId(user.id)
                ?: throw NotFoundException("Preferences not found")
        return PreferenceResponse.from(preference)
    }

    @Transactional
    fun save(
        auth0Sub: String,
        request: SavePreferenceRequest,
    ): PreferenceResponse {
        val user = findOrCreateUser(auth0Sub)
        val existing = userPreferenceFacade.findByUserId(user.id)

        val entity =
            if (existing != null) {
                applyRequest(existing, request)
            } else {
                createNew(user, request)
            }

        return PreferenceResponse.from(userPreferenceFacade.save(entity))
    }

    fun normalize(rawInput: String): PreferenceResponse {
        val result = preferenceNormalizer.normalize(rawInput)
        return PreferenceResponse(
            rawInput = rawInput,
            categories = result.categories,
            seniorityLevels = result.seniorityLevels,
            keywords = result.keywords,
            excludedKeywords = result.excludedKeywords,
            remoteOnly = result.remoteOnly,
            enabledSources = result.enabledSources,
            notificationsEnabled = true,
        )
    }

    private fun createNew(
        user: UserEntity,
        request: SavePreferenceRequest,
    ): UserPreferenceEntity = applyRequest(UserPreferenceEntity(user = user), request)

    private fun applyRequest(
        entity: UserPreferenceEntity,
        request: SavePreferenceRequest,
    ): UserPreferenceEntity {
        entity.rawInput = request.rawInput
        entity.categories = request.categories
        entity.seniorityLevels = request.seniorityLevels
        entity.keywords = request.keywords
        entity.excludedKeywords = request.excludedKeywords
        entity.remoteOnly = request.remoteOnly
        entity.enabledSources = request.enabledSources
        entity.notificationsEnabled = request.notificationsEnabled
        return entity
    }

    private fun findOrCreateUser(auth0Sub: String): UserEntity = userFacade.findOrCreate(auth0Sub)
}
