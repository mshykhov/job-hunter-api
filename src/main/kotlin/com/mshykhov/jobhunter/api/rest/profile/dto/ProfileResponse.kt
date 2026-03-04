package com.mshykhov.jobhunter.api.rest.profile.dto

data class ProfileResponse(
    val aiConfigured: Boolean,
    val preferencesConfigured: Boolean,
    val telegramConfigured: Boolean,
    val warnings: List<ProfileWarning>,
)
