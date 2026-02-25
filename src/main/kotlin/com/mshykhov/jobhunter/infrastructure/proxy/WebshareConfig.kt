package com.mshykhov.jobhunter.infrastructure.proxy

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
@EnableConfigurationProperties(WebshareProperties::class)
class WebshareConfig {
    @Bean
    @ConditionalOnProperty(prefix = "jobhunter.proxy", name = ["enabled"], havingValue = "true")
    fun webshareClient(properties: WebshareProperties): WebshareClient =
        WebshareClient(
            RestClient
                .builder()
                .baseUrl(properties.baseUrl)
                .defaultHeader("Authorization", "Token ${properties.apiKey}")
                .build(),
        )
}
