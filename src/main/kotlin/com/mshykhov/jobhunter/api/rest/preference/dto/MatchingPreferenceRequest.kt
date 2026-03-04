package com.mshykhov.jobhunter.api.rest.preference.dto

data class MatchingPreferenceRequest(
    val keywords: List<String> = emptyList(),
    val excludedKeywords: List<String> = emptyList(),
    val excludedTitleKeywords: List<String> = emptyList(),
    val excludedCompanies: List<String> = emptyList(),
    val seniorityLevels: List<String> = emptyList(),
    val minScore: Int = 50,
    val matchWithAi: Boolean = true,
    val customPrompt: String? = null,
)
