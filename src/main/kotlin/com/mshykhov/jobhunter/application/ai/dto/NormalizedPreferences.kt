package com.mshykhov.jobhunter.application.ai.dto

import com.mshykhov.jobhunter.application.job.JobSource

// categories stay plain strings: value classes (Category) leak into the
// AI-generated JSON schema as {"value": ...} objects the model then mimics.
data class NormalizedPreferences(
    val categories: List<String> = emptyList(),
    val excludedKeywords: List<String> = emptyList(),
    val locations: List<String> = emptyList(),
    val remoteOnly: Boolean = false,
    val disabledSources: List<JobSource> = emptyList(),
)
