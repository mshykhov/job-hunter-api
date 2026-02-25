package com.mshykhov.jobhunter.api.rest.preference.dto

import com.mshykhov.jobhunter.application.job.JobSource
import com.mshykhov.jobhunter.application.preference.UserPreferenceEntity

data class PreferenceResponse(
    val rawInput: String?,
    val categories: List<String>,
    val seniorityLevels: List<String>,
    val keywords: List<String>,
    val excludedKeywords: List<String>,
    val remoteOnly: Boolean,
    val enabledSources: List<JobSource>,
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
                remoteOnly = entity.remoteOnly,
                enabledSources = entity.enabledSources,
                notificationsEnabled = entity.notificationsEnabled,
            )
    }
}
