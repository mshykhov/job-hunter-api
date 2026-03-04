package com.mshykhov.jobhunter.api.rest.preference.dto

import com.mshykhov.jobhunter.application.preference.MatchingPreferences

data class MatchingPreferenceRequest(
    val keywords: List<String> = emptyList(),
    val excludedKeywords: List<String> = emptyList(),
    val excludedTitleKeywords: List<String> = emptyList(),
    val excludedCompanies: List<String> = emptyList(),
    val seniorityLevels: List<String> = emptyList(),
    val minScore: Int = 50,
    val matchWithAi: Boolean = true,
    val customPrompt: String? = null,
) {
    fun applyTo(target: MatchingPreferences) {
        target.keywords = keywords
        target.excludedKeywords = excludedKeywords
        target.excludedTitleKeywords = excludedTitleKeywords
        target.excludedCompanies = excludedCompanies
        target.seniorityLevels = seniorityLevels
        target.minScore = minScore
        target.matchWithAi = matchWithAi
        target.customPrompt = customPrompt
    }
}
