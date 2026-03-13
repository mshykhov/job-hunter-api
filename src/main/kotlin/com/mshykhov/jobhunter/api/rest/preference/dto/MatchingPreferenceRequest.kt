package com.mshykhov.jobhunter.api.rest.preference.dto

import com.mshykhov.jobhunter.application.preference.MatchingPreferences

data class MatchingPreferenceRequest(
    val excludedKeywords: List<String> = emptyList(),
    val excludedTitleKeywords: List<String> = emptyList(),
    val excludedCompanies: List<String> = emptyList(),
    val matchWithAi: Boolean = true,
    val customPrompt: String? = null,
) {
    fun applyTo(target: MatchingPreferences) {
        target.excludedKeywords = excludedKeywords
        target.excludedTitleKeywords = excludedTitleKeywords
        target.excludedCompanies = excludedCompanies
        target.matchWithAi = matchWithAi
        target.customPrompt = customPrompt
    }
}
