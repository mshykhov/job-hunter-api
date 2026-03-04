package com.mshykhov.jobhunter.api.rest.preference.dto

data class TelegramPreferenceRequest(
    val chatId: String? = null,
    val username: String? = null,
    val notificationsEnabled: Boolean = true,
    val notificationSources: List<String> = emptyList(),
)
