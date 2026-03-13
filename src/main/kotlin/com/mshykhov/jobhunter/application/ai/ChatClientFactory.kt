package com.mshykhov.jobhunter.application.ai

import com.mshykhov.jobhunter.application.common.AiNotConfiguredException
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.stereotype.Component

@Component
class ChatClientFactory {
    fun createForUser(settings: UserAiSettingsEntity): ChatClient {
        if (settings.apiKey.isBlank()) {
            throw AiNotConfiguredException("API key is corrupted or missing — please re-enter your API key in settings.")
        }
        val api = OpenAiApi.builder().apiKey(settings.apiKey).build()
        val temperature = if (settings.modelId.contains("nano")) 1.0 else 0.2
        val options =
            OpenAiChatOptions
                .builder()
                .model(settings.modelId)
                .temperature(temperature)
                .build()
        val model =
            OpenAiChatModel
                .builder()
                .openAiApi(api)
                .defaultOptions(options)
                .build()
        return ChatClient.builder(model).build()
    }
}
