package com.mshykhov.jobhunter.application.user

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
class Telegram(
    @Column(name = "telegram_chat_id")
    var chatId: String? = null,
    @Column(name = "telegram_username")
    var username: String? = null,
)
