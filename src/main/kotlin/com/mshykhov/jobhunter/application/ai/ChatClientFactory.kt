package com.mshykhov.jobhunter.application.ai

import com.mshykhov.jobhunter.application.common.AiNotConfiguredException
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.stereotype.Component

@Component
class ChatClientFactory {
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
        // OpenAI reasoning models that use reasoning_effort instead of temperature.
        // Non-OpenAI models (Claude, etc.) go through OpenAI-compatible API and receive temperature.
        private fun isReasoningModel(modelId: String): Boolean =
            modelId.startsWith("gpt-5") ||
                modelId.startsWith("o1") ||
                modelId.startsWith("o3") ||
                modelId.startsWith("o4")
    }
}
