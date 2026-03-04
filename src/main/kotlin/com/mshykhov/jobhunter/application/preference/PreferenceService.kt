package com.mshykhov.jobhunter.application.preference

import com.mshykhov.jobhunter.api.rest.preference.dto.MatchingPreferenceRequest
import com.mshykhov.jobhunter.api.rest.preference.dto.MatchingPreferenceResponse
import com.mshykhov.jobhunter.api.rest.preference.dto.NormalizePreferenceResponse
import com.mshykhov.jobhunter.api.rest.preference.dto.PreferenceResponse
import com.mshykhov.jobhunter.api.rest.preference.dto.SearchPreferenceRequest
import com.mshykhov.jobhunter.api.rest.preference.dto.SearchPreferenceResponse
import com.mshykhov.jobhunter.api.rest.preference.dto.TelegramPreferenceRequest
import com.mshykhov.jobhunter.api.rest.preference.dto.TelegramPreferenceResponse
import com.mshykhov.jobhunter.application.ai.ChatClientFactory
import com.mshykhov.jobhunter.application.ai.PreferenceNormalizer
import com.mshykhov.jobhunter.application.ai.UserAiSettingsService
import com.mshykhov.jobhunter.application.common.NotFoundException
import com.mshykhov.jobhunter.application.user.UserFacade
import com.mshykhov.jobhunter.infrastructure.document.DocumentParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
class PreferenceService(
    private val userFacade: UserFacade,
    private val userPreferenceFacade: UserPreferenceFacade,
    private val preferenceNormalizer: PreferenceNormalizer,
    private val userAiSettingsService: UserAiSettingsService,
    private val chatClientFactory: ChatClientFactory,
    private val documentParser: DocumentParser,
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
    fun saveSearch(
        auth0Sub: String,
        request: SearchPreferenceRequest,
    ): SearchPreferenceResponse {
        val entity = findOrCreatePreference(auth0Sub)
        request.applyTo(entity.search)
        return SearchPreferenceResponse.from(userPreferenceFacade.save(entity).search)
    }

    @Transactional
    fun saveMatching(
        auth0Sub: String,
        request: MatchingPreferenceRequest,
    ): MatchingPreferenceResponse {
        val entity = findOrCreatePreference(auth0Sub)
        request.applyTo(entity.matching)
        return MatchingPreferenceResponse.from(userPreferenceFacade.save(entity).matching)
    }

    @Transactional
    fun saveTelegram(
        auth0Sub: String,
        request: TelegramPreferenceRequest,
    ): TelegramPreferenceResponse {
        val entity = findOrCreatePreference(auth0Sub)
        request.applyTo(entity.telegram)
        return TelegramPreferenceResponse.from(userPreferenceFacade.save(entity).telegram)
    }

    fun normalizeFile(
        auth0Sub: String,
        file: MultipartFile,
    ): NormalizePreferenceResponse {
        val contentType =
            file.contentType
                ?: throw IllegalArgumentException("File content type is required")
        val text = documentParser.extractText(file.inputStream, contentType)
        return normalize(auth0Sub, text)
    }

    fun normalize(
        auth0Sub: String,
        rawInput: String,
    ): NormalizePreferenceResponse {
        val settings = userAiSettingsService.resolveForUser(auth0Sub)
        val chatClient = chatClientFactory.createForUser(settings)
        val result = preferenceNormalizer.normalize(rawInput, chatClient)
        return NormalizePreferenceResponse.from(rawInput, result)
    }

    private fun findOrCreatePreference(auth0Sub: String): UserPreferenceEntity {
        val user = userFacade.findOrCreate(auth0Sub)
        return userPreferenceFacade.findByUserId(user.id)
            ?: UserPreferenceEntity(user = user)
    }
}
