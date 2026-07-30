package com.mshykhov.jobhunter.api.rest.settings.dto

internal fun maskApiKey(apiKey: String): String =
    if (apiKey.length > 8) {
        "${apiKey.take(8)}${"*".repeat(apiKey.length - 8)}"
    } else {
        "*".repeat(apiKey.length)
    }
