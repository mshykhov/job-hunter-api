package com.mshykhov.jobhunter.api.rest.preference.dto

import com.mshykhov.jobhunter.application.preference.UserPreferenceEntity

data class PreferenceResponse(
    val rawInput: String?,
    val categories: List<String>,
    val seniorityLevels: List<String>,
    val keywords: List<String>,
    val excludedKeywords: List<String>,
    val minSalary: Int?,
    val remoteOnly: Boolean,
    val enabledSources: List<String>,
    val notificationsEnabled: Boolean,
) {
    companion object {
        fun from(entity: UserPreferenceEntity): PreferenceResponse =
            PreferenceResponse(
                rawInput = entity.rawInput,
                categories = entity.categories,
                seniorityLevels = entity.seniorityLevels,
                keywords = entity.keywords,
                excludedKeywords = entity.excludedKeywords,
                minSalary = entity.minSalary,
                remoteOnly = entity.remoteOnly,
                enabledSources = entity.enabledSources,
                notificationsEnabled = entity.notificationsEnabled,
            )
    }
}
