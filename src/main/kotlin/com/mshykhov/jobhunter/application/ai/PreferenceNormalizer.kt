package com.mshykhov.jobhunter.application.ai

import com.mshykhov.jobhunter.api.rest.exception.custom.ServiceUnavailableException
import com.mshykhov.jobhunter.application.ai.dto.NormalizedPreferences
import org.springframework.ai.chat.client.ChatClient
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Service

@Service
class PreferenceNormalizer(
    private val chatClientProvider: ObjectProvider<ChatClient>,
) {
    fun normalize(rawInput: String): NormalizedPreferences {
        val chatClient =
            chatClientProvider.ifAvailable
                ?: throw ServiceUnavailableException("AI service unavailable")
        return try {
            chatClient
                .prompt()
                .system(SYSTEM_PROMPT)
                .user(rawInput)
                .call()
                .entity(NormalizedPreferences::class.java)
                ?: throw ServiceUnavailableException("Failed to parse AI response")
        } catch (e: ServiceUnavailableException) {
            throw e
        } catch (e: Exception) {
            throw ServiceUnavailableException("AI normalization failed: ${e.message}")
        }
    }
}

private val SYSTEM_PROMPT =
    """
    You are a job search preferences analyzer. Extract structured data from the user's free-text input.

    Extract the following:
    - categories: core technologies the user wants to work with (e.g. kotlin, java, javascript, python, go, rust, typescript)
    - seniorityLevels: experience levels (e.g. junior, middle, senior, lead, principal)
    - keywords: relevant skill/framework keywords for job matching (e.g. spring, react, kubernetes, microservices)
    - excludedKeywords: technologies or domains the user wants to avoid
    - remoteOnly: whether user wants only remote positions (true/false)
    """.trimIndent()
