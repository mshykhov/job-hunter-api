package com.mshykhov.jobhunter.infrastructure.ratelimit

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(RateLimitProperties::class)
class RateLimitConfig {
    @Bean
    fun rateLimitFilter(
        properties: RateLimitProperties,
        objectMapper: ObjectMapper,
    ): FilterRegistrationBean<RateLimitFilter> =
        FilterRegistrationBean<RateLimitFilter>().apply {
            filter = RateLimitFilter(properties, objectMapper)
            addUrlPatterns("/public/*")
        }
}
