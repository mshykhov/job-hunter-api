package com.mshykhov.jobhunter.application.preference

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Embeddable
class TelegramPreferences(
    @Column(name = "telegram_chat_id")
    var chatId: String? = null,
    @Column(name = "telegram_username")
    var username: String? = null,
    @Column(name = "notifications_enabled", nullable = false)
    var notificationsEnabled: Boolean = true,
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "notification_sources", columnDefinition = "text[]")
    var notificationSources: List<String> = emptyList(),
)
