package com.mshykhov.jobhunter.api.rest.settings.dto

internal fun maskApiKey(
    apiKey: String,
    requiresApiKey: Boolean = true,
): String =
    when {
        apiKey.isBlank() && !requiresApiKey -> "No API key required"
        apiKey.length > 8 -> "${apiKey.take(8)}${"*".repeat(apiKey.length - 8)}"
        else -> "*".repeat(apiKey.length)
    }
