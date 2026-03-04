package com.mshykhov.jobhunter.api.rest.preference.dto

import com.mshykhov.jobhunter.application.preference.TelegramPreferences

data class TelegramPreferenceRequest(
    val chatId: String? = null,
    val username: String? = null,
    val notificationsEnabled: Boolean = true,
    val notificationSources: List<String> = emptyList(),
) {
    fun applyTo(target: TelegramPreferences) {
        target.chatId = chatId
        target.username = username
        target.notificationsEnabled = notificationsEnabled
        target.notificationSources = notificationSources
    }
}
