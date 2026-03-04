package com.mshykhov.jobhunter.api.rest.preference.dto

import com.mshykhov.jobhunter.application.job.JobSource

data class MatchingPreferenceRequest(
    val keywords: List<String> = emptyList(),
    val excludedKeywords: List<String> = emptyList(),
    val excludedTitleKeywords: List<String> = emptyList(),
    val excludedCompanies: List<String> = emptyList(),
    val disabledSources: List<JobSource> = emptyList(),
    val minScore: Int = 50,
    val matchWithAi: Boolean = true,
    val customPrompt: String? = null,
)
