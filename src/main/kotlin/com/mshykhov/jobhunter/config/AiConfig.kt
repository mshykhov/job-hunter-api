package com.mshykhov.jobhunter.config

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(AiProperties::class)
class AiConfig {
    @Bean
    @ConditionalOnProperty(prefix = "jobhunter.ai", name = ["enabled"], havingValue = "true")
    fun anthropicClient(properties: AiProperties): AnthropicClient {
        val builder =
            AnthropicOkHttpClient
                .builder()
                .baseUrl(properties.baseUrl)
        if (properties.apiKey.isNotBlank()) {
            builder.apiKey(properties.apiKey)
        }
        if (properties.authToken.isNotBlank()) {
            builder.authToken(properties.authToken)
        }
        return builder.build()
    }
}
