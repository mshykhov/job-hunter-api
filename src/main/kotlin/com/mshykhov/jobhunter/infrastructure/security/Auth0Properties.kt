package com.mshykhov.jobhunter.infrastructure.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jobhunter.auth0")
data class Auth0Properties(
    val enabled: Boolean = true,
    val issuer: String = "",
    val audience: String = "",
)
