package com.mshykhov.jobhunter.api.rest.preference.dto

import com.mshykhov.jobhunter.application.preference.MatchingPreferences

data class MatchingPreferenceResponse(
    val excludedKeywords: List<String>,
    val excludedTitleKeywords: List<String>,
    val excludedCompanies: List<String>,
    val matchWithAi: Boolean,
    val customPrompt: String?,
) {
    companion object {
        fun from(matching: MatchingPreferences): MatchingPreferenceResponse =
            MatchingPreferenceResponse(
                excludedKeywords = matching.excludedKeywords,
                excludedTitleKeywords = matching.excludedTitleKeywords,
                excludedCompanies = matching.excludedCompanies,
                matchWithAi = matching.matchWithAi,
                customPrompt = matching.customPrompt,
            )
    }
}
