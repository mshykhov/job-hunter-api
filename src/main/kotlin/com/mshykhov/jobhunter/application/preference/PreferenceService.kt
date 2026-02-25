package com.mshykhov.jobhunter.application.preference

import com.fasterxml.jackson.databind.ObjectMapper
import com.mshykhov.jobhunter.api.rest.preference.dto.PreferenceResponse
import com.mshykhov.jobhunter.api.rest.preference.dto.SavePreferenceRequest
import com.mshykhov.jobhunter.api.rest.exception.custom.ServiceUnavailableException
import com.mshykhov.jobhunter.api.rest.exception.custom.NotFoundException
import com.mshykhov.jobhunter.application.user.UserEntity
import com.mshykhov.jobhunter.application.user.UserFacade
import com.mshykhov.jobhunter.infrastructure.ai.AiProperties
import com.mshykhov.jobhunter.infrastructure.ai.ClaudeClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PreferenceService(
    private val userFacade: UserFacade,
    private val userPreferenceFacade: UserPreferenceFacade,
    private val claudeClient: ClaudeClient,
    private val aiProperties: AiProperties,
    private val objectMapper: ObjectMapper,
) {
    @Transactional(readOnly = true)
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

    fun normalize(rawInput: String): PreferenceResponse {
        val prompt = buildNormalizePrompt(rawInput)
        val response =
            claudeClient.sendMessage(prompt, aiProperties.normalize.maxTokens)
                ?: throw ServiceUnavailableException("AI service unavailable")
        return parseNormalizeResponse(rawInput, response)
    }

    // --- CRUD helpers ---

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

    private fun findUser(auth0Sub: String): UserEntity =
        userFacade.findByAuth0Sub(auth0Sub)
            ?: throw NotFoundException("User not found: $auth0Sub")

    private fun findOrCreateUser(auth0Sub: String): UserEntity = userFacade.findOrCreate(auth0Sub)

    // --- AI normalization ---

    private fun buildNormalizePrompt(rawInput: String): String =
        """
        |You are a job search preferences analyzer. Extract structured data from the user's free-text input.
        |
        |User input: $rawInput
        |
        |Analyze the text and extract:
        |- categories: core technologies the user wants to work with (e.g. kotlin, java, javascript, python, go, rust, typescript)
        |- seniorityLevels: experience levels (e.g. junior, middle, senior, lead, principal)
        |- keywords: relevant skill/framework keywords for job matching (e.g. spring, react, kubernetes, microservices)
        |- excludedKeywords: technologies or domains the user wants to avoid
        |- remoteOnly: whether user wants only remote positions (true/false)
        |
        |Return ONLY a valid JSON object with no additional text:
        |{
        |  "categories": [],
        |  "seniorityLevels": [],
        |  "keywords": [],
        |  "excludedKeywords": [],
        |  "remoteOnly": false
        |}
        """.trimMargin()

    private fun parseNormalizeResponse(
        rawInput: String,
        response: String,
    ): PreferenceResponse {
        val json =
            ClaudeClient.extractJson(response)
                ?: throw ServiceUnavailableException("Failed to parse AI response: no valid JSON found")
        val tree = objectMapper.readTree(json)
        return PreferenceResponse(
            rawInput = rawInput,
            categories = tree.get("categories")?.map { it.asText() } ?: emptyList(),
            seniorityLevels = tree.get("seniorityLevels")?.map { it.asText() } ?: emptyList(),
            keywords = tree.get("keywords")?.map { it.asText() } ?: emptyList(),
            excludedKeywords = tree.get("excludedKeywords")?.map { it.asText() } ?: emptyList(),
            remoteOnly = tree.get("remoteOnly")?.asBoolean() ?: false,
            enabledSources = emptyList(),
            notificationsEnabled = true,
        )
    }
}
