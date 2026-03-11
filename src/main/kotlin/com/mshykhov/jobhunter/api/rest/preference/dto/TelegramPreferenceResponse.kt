package com.mshykhov.jobhunter.api.rest.preference.dto

import com.mshykhov.jobhunter.application.preference.TelegramPreferences

data class TelegramPreferenceResponse(val chatId: String?, val username: String?, val notificationsEnabled: Boolean, val notificationSources: List<String>) {
    companion object {
        fun from(telegram: TelegramPreferences): TelegramPreferenceResponse =
            TelegramPreferenceResponse(
                chatId = telegram.chatId,
                username = telegram.username,
                notificationsEnabled = telegram.notificationsEnabled,
                notificationSources = telegram.notificationSources,
            )
    }
}
