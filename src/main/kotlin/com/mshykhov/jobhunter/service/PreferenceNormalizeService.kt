package com.mshykhov.jobhunter.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.mshykhov.jobhunter.config.AiProperties
import com.mshykhov.jobhunter.controller.preference.dto.PreferenceResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class PreferenceNormalizeService(
    private val claudeClient: ClaudeClient,
    private val aiProperties: AiProperties,
    private val objectMapper: ObjectMapper,
) {
    fun normalize(rawInput: String): PreferenceResponse =
        try {
            val prompt = buildPrompt(rawInput)
            val response = claudeClient.sendMessage(prompt, aiProperties.normalize.maxTokens)
            if (response != null) parseResponse(rawInput, response) else fallbackResponse(rawInput)
        } catch (e: Exception) {
            logger.error(e) { "Preference normalization failed" }
            fallbackResponse(rawInput)
        }

    private fun buildPrompt(rawInput: String): String =
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

    private fun parseResponse(
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
