package com.mshykhov.jobhunter.api.rest.preference.dto

data class SavePreferenceRequest(
    val rawInput: String? = null,
    val categories: List<String> = emptyList(),
    val seniorityLevels: List<String> = emptyList(),
    val keywords: List<String> = emptyList(),
    val excludedKeywords: List<String> = emptyList(),
    val minSalary: Int? = null,
    val remoteOnly: Boolean = false,
    val enabledSources: List<String> = emptyList(),
    val notificationsEnabled: Boolean = true,
)
