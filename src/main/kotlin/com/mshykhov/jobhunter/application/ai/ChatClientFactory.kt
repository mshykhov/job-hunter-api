package com.mshykhov.jobhunter.application.ai

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.stereotype.Component

@Component
class ChatClientFactory {
    fun createForUser(settings: UserAiSettingsEntity): ChatClient {
        val api = OpenAiApi.builder().apiKey(settings.apiKey).build()
        val options =
            OpenAiChatOptions
                .builder()
                .model(settings.modelId)
                .temperature(1.0)
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
