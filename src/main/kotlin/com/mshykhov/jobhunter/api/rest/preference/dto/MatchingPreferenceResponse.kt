package com.mshykhov.jobhunter.api.rest.preference.dto

import com.mshykhov.jobhunter.application.preference.MatchingPreferences

data class MatchingPreferenceResponse(
    val keywords: List<String>,
    val excludedKeywords: List<String>,
    val excludedTitleKeywords: List<String>,
    val excludedCompanies: List<String>,
    val seniorityLevels: List<String>,
    val matchWithAi: Boolean,
    val customPrompt: String?,
    val weightTechnology: Int,
    val weightSeniority: Int,
    val weightSkills: Int,
) {
    companion object {
        fun from(matching: MatchingPreferences): MatchingPreferenceResponse =
            MatchingPreferenceResponse(
                keywords = matching.keywords,
                excludedKeywords = matching.excludedKeywords,
                excludedTitleKeywords = matching.excludedTitleKeywords,
                excludedCompanies = matching.excludedCompanies,
                seniorityLevels = matching.seniorityLevels,
                matchWithAi = matching.matchWithAi,
                customPrompt = matching.customPrompt,
                weightTechnology = matching.weightTechnology,
                weightSeniority = matching.weightSeniority,
                weightSkills = matching.weightSkills,
            )
    }
}
