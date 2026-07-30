package com.mshykhov.jobhunter.application.ai

import com.mshykhov.jobhunter.application.common.AiNotConfiguredException
import com.mshykhov.jobhunter.application.settings.AiProvider
import com.mshykhov.jobhunter.infrastructure.ai.AiProviderProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class ChatClientFactory(private val aiProviderProperties: AiProviderProperties) {
    fun createForUser(
        settings: UserAiSettingsEntity,
        useCase: AiUseCase = AiUseCase.SCORING,
    ): ChatClient {
        if (settings.apiKey.isBlank()) {
            throw AiNotConfiguredException("API key is corrupted or missing — please re-enter your API key in settings.")
        }
        val api = OpenAiApi.builder().apiKey(settings.apiKey).build()
        val options = buildOptions(settings.modelId, useCase)
        val model =
            OpenAiChatModel
                .builder()
                .openAiApi(api)
                .defaultOptions(options)
                .build()
        return ChatClient.builder(model).build()
    }

    fun createForProvider(
        provider: UserAiProviderEntity,
        useCase: AiUseCase = AiUseCase.SCORING,
    ): ChatClient {
        val apiKey = resolveApiKey(provider)
        val builder = OpenAiApi.builder().apiKey(apiKey)
        aiProviderProperties.baseUrlFor(provider.provider)?.let { builder.baseUrl(it) }
        val model =
            OpenAiChatModel
                .builder()
                .openAiApi(builder.build())
                .defaultOptions(buildOptions(provider.modelId, useCase))
                .build()
        return ChatClient.builder(model).build()
    }

    fun createChain(
        providers: List<UserAiProviderEntity>,
        useCase: AiUseCase = AiUseCase.SCORING,
    ): AiClientChain =
        AiClientChain(
            providers
                .filter { it.enabled }
                .sortedBy { it.priority }
                .mapNotNull { provider -> buildLinkOrNull(provider, useCase) },
        )

    private fun buildLinkOrNull(
        provider: UserAiProviderEntity,
        useCase: AiUseCase,
    ): AiClientLink? =
        try {
            AiClientLink(provider.provider, provider.modelId, createForProvider(provider, useCase))
        } catch (e: Exception) {
            logger.warn(e) { "Skipping AI provider ${provider.provider} at priority ${provider.priority}: ${e.message}" }
            null
        }

    private fun resolveApiKey(provider: UserAiProviderEntity): String =
        if (provider.provider.requiresApiKey) {
            provider.apiKey.ifBlank {
                throw AiNotConfiguredException(
                    "API key is missing for ${provider.provider.displayName} - please add one in settings.",
                )
            }
        } else if (aiProviderProperties.baseUrlFor(provider.provider) == null) {
            throw AiNotConfiguredException(
                "No base URL configured for ${provider.provider.displayName} - set ${baseUrlEnvVarFor(provider.provider)}.",
            )
        } else {
            DUMMY_API_KEY
        }

    private fun baseUrlEnvVarFor(provider: AiProvider): String =
        when (provider) {
            AiProvider.CODEX -> "AI_CODEX_BASE_URL"
            AiProvider.GEMINI -> "AI_GEMINI_BASE_URL"
            AiProvider.OPENAI -> "AI_OPENAI_BASE_URL"
        }

    private fun buildOptions(
        modelId: String,
        useCase: AiUseCase,
    ): OpenAiChatOptions {
        val builder = OpenAiChatOptions.builder().model(modelId)
        if (isReasoningModel(modelId)) {
            builder.reasoningEffort(useCase.reasoningEffort)
        } else {
            builder.temperature(useCase.temperature)
        }
        return builder.build()
    }

    companion object {
        private const val DUMMY_API_KEY = "dummy"

        // OpenAI reasoning models that use reasoning_effort instead of temperature.
        // Non-OpenAI models (Claude, etc.) go through OpenAI-compatible API and receive temperature.
        internal fun isReasoningModel(modelId: String): Boolean =
            modelId.startsWith("gpt-5") ||
                modelId.startsWith("o1") ||
                modelId.startsWith("o3") ||
                modelId.startsWith("o4")
    }
}
