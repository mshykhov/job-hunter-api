package com.mshykhov.jobhunter.api.rest.preference.dto

import com.mshykhov.jobhunter.application.job.JobSource
import com.mshykhov.jobhunter.application.preference.MatchingPreferences

data class MatchingPreferenceResponse(
    val keywords: List<String>,
    val excludedKeywords: List<String>,
    val excludedTitleKeywords: List<String>,
    val excludedCompanies: List<String>,
    val disabledSources: List<JobSource>,
    val minScore: Int,
    val matchWithAi: Boolean,
    val customPrompt: String?,
) {
    companion object {
        fun from(matching: MatchingPreferences): MatchingPreferenceResponse =
            MatchingPreferenceResponse(
                keywords = matching.keywords,
                excludedKeywords = matching.excludedKeywords,
                excludedTitleKeywords = matching.excludedTitleKeywords,
                excludedCompanies = matching.excludedCompanies,
                disabledSources = matching.disabledSources,
                minScore = matching.minScore,
                matchWithAi = matching.matchWithAi,
                customPrompt = matching.customPrompt,
            )
    }
}
