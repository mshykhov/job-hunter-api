package com.mshykhov.jobhunter.service

import com.anthropic.client.AnthropicClient
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import com.anthropic.models.messages.TextBlock
import com.mshykhov.jobhunter.config.AiProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class ClaudeClient(
    private val clientProvider: ObjectProvider<AnthropicClient>,
    private val aiProperties: AiProperties,
) {
    fun sendMessage(
        userPrompt: String,
        maxTokens: Long,
    ): String? {
        val client =
            clientProvider.ifAvailable ?: run {
                logger.warn { "AnthropicClient not available, AI features disabled" }
                return null
            }
        val params =
            MessageCreateParams
                .builder()
                .maxTokens(maxTokens)
                .addUserMessage(userPrompt)
                .model(Model.of(aiProperties.model))
                .build()
        val message = client.messages().create(params)
        return message
            .content()
            .filterIsInstance<TextBlock>()
            .firstOrNull()
            ?.text()
    }
}
