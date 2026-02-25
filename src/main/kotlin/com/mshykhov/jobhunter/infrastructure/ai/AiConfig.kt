package com.mshykhov.jobhunter.infrastructure.ai

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.model.ChatModel
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(AiProperties::class)
class AiConfig {
    @Bean
    @ConditionalOnBean(ChatModel::class)
    fun chatClient(builder: ChatClient.Builder): ChatClient = builder.build()
}
