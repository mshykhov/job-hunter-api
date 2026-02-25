package com.mshykhov.jobhunter.api.rest.preference.dto

import com.mshykhov.jobhunter.application.job.JobSource

data class SavePreferenceRequest(
    val rawInput: String? = null,
    val categories: List<String> = emptyList(),
    val seniorityLevels: List<String> = emptyList(),
    val keywords: List<String> = emptyList(),
    val excludedKeywords: List<String> = emptyList(),
    val remoteOnly: Boolean = false,
    val disabledSources: List<JobSource> = emptyList(),
    val minScore: Int = 50,
    val notificationsEnabled: Boolean = true,
)
