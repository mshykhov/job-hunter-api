package com.mshykhov.jobhunter.application.ai.dto

import com.mshykhov.jobhunter.application.job.JobSource

data class NormalizedPreferences(
    val categories: List<String> = emptyList(),
    val seniorityLevels: List<String> = emptyList(),
    val keywords: List<String> = emptyList(),
    val excludedKeywords: List<String> = emptyList(),
    val remoteOnly: Boolean = false,
    val disabledSources: List<JobSource> = emptyList(),
)
