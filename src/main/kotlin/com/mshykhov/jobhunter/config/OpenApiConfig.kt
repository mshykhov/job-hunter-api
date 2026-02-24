package com.mshykhov.jobhunter.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    @Bean
    fun openApi(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("Job Hunter API")
                    .description("Job vacancy monitoring and tracking system")
                    .version("0.1.0")
                    .contact(Contact().name("mshykhov").url("https://github.com/mshykhov/job-hunter")),
            )
}
