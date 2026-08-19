package com.mshykhov.jobhunter.infrastructure.automation

import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

@Component
class AutomationIdentityGuard(private val properties: AutomationProperties) {
    fun requireOwner(
        jwt: Jwt,
        authority: String,
    ) {
        requireEnabled()
        requireMatch(jwt.issuer?.toString().orEmpty(), properties.ownerIssuer)
        requireMatch(jwt.subject, properties.ownerSubject)
        requireAuthority(jwt, authority)
    }

    fun requireRunner(jwt: Jwt) {
        requireEnabled()
        requireMatch(jwt.issuer?.toString().orEmpty(), properties.runnerIssuer)
        requireAuthority(jwt, REPORT_AUTHORITY)
    }

    private fun requireEnabled() {
        if (!properties.enabled) deny()
    }

    private fun requireMatch(
        actual: String,
        expected: String,
    ) {
        if (!secureEquals(actual, expected)) deny()
    }

    private fun requireAuthority(
        jwt: Jwt,
        authority: String,
    ) {
        val scopes = jwt.getClaimAsStringList("scope") ?: jwt.getClaimAsString("scope")?.split(" ") ?: emptyList()
        val permissions = jwt.getClaimAsStringList("permissions") ?: emptyList()
        if (authority !in scopes && authority !in permissions) deny()
    }

    private fun secureEquals(
        actual: String,
        expected: String,
    ): Boolean {
        if (actual.length != expected.length) return false
        return MessageDigest.isEqual(
            actual.toByteArray(StandardCharsets.UTF_8),
            expected.toByteArray(StandardCharsets.UTF_8),
        )
    }

    private fun deny(): Nothing = throw AuthorizationDeniedException("Access denied")

    private companion object {
        const val REPORT_AUTHORITY = "report:automation-health"
    }
}
