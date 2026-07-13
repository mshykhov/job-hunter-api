package com.mshykhov.jobhunter.infrastructure.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jobhunter.oidc")
data class OidcProperties(val enabled: Boolean = true, val issuers: List<String> = emptyList(), val audience: String = "")
