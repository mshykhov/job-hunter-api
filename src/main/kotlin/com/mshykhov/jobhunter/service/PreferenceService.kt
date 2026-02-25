package com.mshykhov.jobhunter.service

import com.mshykhov.jobhunter.controller.preference.dto.PreferenceResponse
import com.mshykhov.jobhunter.controller.preference.dto.SavePreferenceRequest
import com.mshykhov.jobhunter.exception.NotFoundException
import com.mshykhov.jobhunter.persistence.facade.UserFacade
import com.mshykhov.jobhunter.persistence.facade.UserPreferenceFacade
import com.mshykhov.jobhunter.persistence.model.UserEntity
import com.mshykhov.jobhunter.persistence.model.UserPreferenceEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PreferenceService(
    private val userFacade: UserFacade,
    private val userPreferenceFacade: UserPreferenceFacade,
) {
    fun get(auth0Sub: String): PreferenceResponse {
        val user = findUser(auth0Sub)
        val preference =
            userPreferenceFacade.findByUserId(user.id)
                ?: throw NotFoundException("Preferences not found for user ${user.id}")
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

    private fun createNew(
        user: UserEntity,
        request: SavePreferenceRequest,
    ): UserPreferenceEntity =
        UserPreferenceEntity(
            user = user,
            rawInput = request.rawInput,
            categories = request.categories,
            seniorityLevels = request.seniorityLevels,
            keywords = request.keywords,
            excludedKeywords = request.excludedKeywords,
            minSalary = request.minSalary,
            remoteOnly = request.remoteOnly,
            enabledSources = request.enabledSources,
            notificationsEnabled = request.notificationsEnabled,
        )

    private fun applyRequest(
        entity: UserPreferenceEntity,
        request: SavePreferenceRequest,
    ): UserPreferenceEntity {
        entity.rawInput = request.rawInput
        entity.categories = request.categories
        entity.seniorityLevels = request.seniorityLevels
        entity.keywords = request.keywords
        entity.excludedKeywords = request.excludedKeywords
        entity.minSalary = request.minSalary
        entity.remoteOnly = request.remoteOnly
        entity.enabledSources = request.enabledSources
        entity.notificationsEnabled = request.notificationsEnabled
        return entity
    }

    private fun findUser(auth0Sub: String): UserEntity =
        userFacade.findByAuth0Sub(auth0Sub)
            ?: throw NotFoundException("User not found: $auth0Sub")

    private fun findOrCreateUser(auth0Sub: String): UserEntity =
        userFacade.findByAuth0Sub(auth0Sub)
            ?: userFacade.save(UserEntity(auth0Sub = auth0Sub))
}
