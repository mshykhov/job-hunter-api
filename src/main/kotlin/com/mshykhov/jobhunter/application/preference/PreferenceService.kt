package com.mshykhov.jobhunter.application.preference

import com.fasterxml.jackson.databind.ObjectMapper
import com.mshykhov.jobhunter.api.rest.preference.dto.PreferenceResponse
import com.mshykhov.jobhunter.api.rest.preference.dto.SavePreferenceRequest
import com.mshykhov.jobhunter.application.common.NotFoundException
import com.mshykhov.jobhunter.application.preference.UserPreferenceEntity
import com.mshykhov.jobhunter.application.preference.UserPreferenceFacade
import com.mshykhov.jobhunter.application.user.UserEntity
import com.mshykhov.jobhunter.application.user.UserFacade
import com.mshykhov.jobhunter.infrastructure.ai.AiProperties
import com.mshykhov.jobhunter.infrastructure.ai.ClaudeClient
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
class PreferenceService(
    private val userFacade: UserFacade,
    private val userPreferenceFacade: UserPreferenceFacade,
    private val claudeClient: ClaudeClient,
    private val aiProperties: AiProperties,
    private val objectMapper: ObjectMapper,
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

    fun normalize(rawInput: String): PreferenceResponse =
        try {
            val prompt = buildNormalizePrompt(rawInput)
            val response = claudeClient.sendMessage(prompt, aiProperties.normalize.maxTokens)
            if (response != null) parseNormalizeResponse(rawInput, response) else fallbackResponse(rawInput)
        } catch (e: Exception) {
            logger.error(e) { "Preference normalization failed" }
            fallbackResponse(rawInput)
        }

    // --- CRUD helpers ---

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

    // --- AI normalization ---

    private fun buildNormalizePrompt(rawInput: String): String =
        """
        |You are a job search preferences analyzer. Extract structured data from the user's free-text input.
        |
        |User input: $rawInput
        |
        |Analyze the text and extract:
        |- categories: job domains (e.g. backend, frontend, devops, fullstack, mobile, data)
        |- seniorityLevels: experience levels (e.g. junior, middle, senior, lead, principal)
        |- keywords: relevant technology/skill keywords for job matching
        |- excludedKeywords: technologies or domains the user wants to avoid
        |- minSalary: minimum salary in USD (null if not mentioned)
        |- remoteOnly: whether user wants only remote positions (true/false)
        |
        |Return ONLY a valid JSON object with no additional text:
        |{
        |  "categories": [],
        |  "seniorityLevels": [],
        |  "keywords": [],
        |  "excludedKeywords": [],
        |  "minSalary": null,
        |  "remoteOnly": false
        |}
        """.trimMargin()

    private fun parseNormalizeResponse(
        rawInput: String,
        response: String,
    ): PreferenceResponse {
        val json = extractJson(response) ?: return fallbackResponse(rawInput)
        val tree = objectMapper.readTree(json)
        return PreferenceResponse(
            rawInput = rawInput,
            categories = tree.get("categories")?.map { it.asText() } ?: emptyList(),
            seniorityLevels = tree.get("seniorityLevels")?.map { it.asText() } ?: emptyList(),
            keywords = tree.get("keywords")?.map { it.asText() } ?: emptyList(),
            excludedKeywords = tree.get("excludedKeywords")?.map { it.asText() } ?: emptyList(),
            minSalary = tree.get("minSalary")?.takeIf { !it.isNull }?.asInt(),
            remoteOnly = tree.get("remoteOnly")?.asBoolean() ?: false,
            enabledSources = emptyList(),
            notificationsEnabled = true,
        )
    }

    private fun extractJson(text: String): String? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end < 0 || end <= start) return null
        return text.substring(start, end + 1)
    }

    private fun fallbackResponse(rawInput: String): PreferenceResponse =
        PreferenceResponse(
            rawInput = rawInput,
            categories = emptyList(),
            seniorityLevels = emptyList(),
            keywords = emptyList(),
            excludedKeywords = emptyList(),
            minSalary = null,
            remoteOnly = false,
            enabledSources = emptyList(),
            notificationsEnabled = true,
        )
}
