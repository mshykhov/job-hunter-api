package com.mshykhov.jobhunter.infrastructure.security

import io.swagger.v3.oas.models.security.Scopes

object ApiScopes {
    private val all =
        mapOf(
            "read:jobs" to "Read job listings and user job matches",
            "write:jobs" to "Ingest, check, and manage jobs",
            "read:criteria" to "Read aggregated search criteria",
            "read:preferences" to "Read user preferences",
            "write:preferences" to "Create and update user preferences",
            "read:proxies" to "Read proxy list for scraping",
        )

    fun toOpenApiScopes(): Scopes =
        Scopes().apply {
            all.forEach { (name, description) -> addString(name, description) }
        }
}
