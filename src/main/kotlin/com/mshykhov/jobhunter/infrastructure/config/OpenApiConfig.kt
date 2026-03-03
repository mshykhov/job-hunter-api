package com.mshykhov.jobhunter.infrastructure.config

import com.mshykhov.jobhunter.infrastructure.security.Auth0Properties
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.OAuthFlow
import io.swagger.v3.oas.models.security.OAuthFlows
import io.swagger.v3.oas.models.security.Scopes
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig(
    private val auth0Properties: Auth0Properties,
) {
    @Bean
    fun openApi(): OpenAPI {
        val openAPI =
            OpenAPI()
                .info(
                    Info()
                        .title("Job Hunter API")
                        .description("Job vacancy monitoring and tracking system")
                        .version("0.1.0")
                        .contact(Contact().name("mshykhov").url("https://github.com/mshykhov/job-hunter")),
                )

        if (auth0Properties.enabled) {
            val issuer = auth0Properties.issuer.trimEnd('/')
            openAPI
                .addSecurityItem(SecurityRequirement().addList("oauth2"))
                .components(
                    Components().addSecuritySchemes(
                        "oauth2",
                        SecurityScheme()
                            .type(SecurityScheme.Type.OAUTH2)
                            .flows(
                                OAuthFlows().authorizationCode(
                                    OAuthFlow()
                                        .authorizationUrl("$issuer/authorize?audience=${auth0Properties.audience}")
                                        .tokenUrl("$issuer/oauth/token")
                                        .scopes(Scopes()),
                                ),
                            ),
                    ),
                )
        }

        return openAPI
    }
}
