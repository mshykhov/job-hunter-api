package com.mshykhov.jobhunter.api.rest.preference.dto

import com.mshykhov.jobhunter.application.preference.MatchingPreferences
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class MatchingPreferenceRequest(
    val keywords: List<String> = emptyList(),
    val excludedKeywords: List<String> = emptyList(),
    val excludedTitleKeywords: List<String> = emptyList(),
    val excludedCompanies: List<String> = emptyList(),
    val seniorityLevels: List<String> = emptyList(),
    val matchWithAi: Boolean = true,
    val customPrompt: String? = null,
    @field:Min(0) @field:Max(100)
    val weightKeywords: Int = MatchingPreferences.DEFAULT_WEIGHT_KEYWORDS,
    @field:Min(0) @field:Max(100)
    val weightSeniority: Int = MatchingPreferences.DEFAULT_WEIGHT_SENIORITY,
    @field:Min(0) @field:Max(100)
    val weightCategories: Int = MatchingPreferences.DEFAULT_WEIGHT_CATEGORIES,
) {
    @AssertTrue(message = "Scoring weights must sum to 100")
    fun isWeightsSumValid(): Boolean = weightKeywords + weightSeniority + weightCategories == 100

    fun applyTo(target: MatchingPreferences) {
        target.keywords = keywords
        target.excludedKeywords = excludedKeywords
        target.excludedTitleKeywords = excludedTitleKeywords
        target.excludedCompanies = excludedCompanies
        target.seniorityLevels = seniorityLevels
        target.matchWithAi = matchWithAi
        target.customPrompt = customPrompt
        target.weightKeywords = weightKeywords
        target.weightSeniority = weightSeniority
        target.weightCategories = weightCategories
    }
}
