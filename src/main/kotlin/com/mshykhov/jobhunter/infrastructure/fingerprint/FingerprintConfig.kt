package com.mshykhov.jobhunter.infrastructure.fingerprint

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
@EnableConfigurationProperties(ScrapeOpsProperties::class)
class FingerprintConfig {
    @Bean
    @ConditionalOnProperty(prefix = "jobhunter.scrapeops", name = ["enabled"], havingValue = "true")
    fun fingerprintProvider(properties: ScrapeOpsProperties): FingerprintProvider =
        FingerprintProvider(
            restClient = RestClient.builder().baseUrl(properties.baseUrl).build(),
            properties = properties,
        )
}
