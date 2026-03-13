package com.mshykhov.jobhunter.api.rest.preference.dto

import com.mshykhov.jobhunter.application.preference.MatchingPreferences
import jakarta.validation.constraints.Size

data class MatchingPreferenceRequest(
    val excludedKeywords: List<String> = emptyList(),
    val excludedTitleKeywords: List<String> = emptyList(),
    val excludedCompanies: List<String> = emptyList(),
    val matchWithAi: Boolean = true,
    @field:Size(max = MAX_CUSTOM_PROMPT_LENGTH, message = "Custom prompt must not exceed $MAX_CUSTOM_PROMPT_LENGTH characters")
    val customPrompt: String? = null,
) {
    fun applyTo(target: MatchingPreferences) {
        target.excludedKeywords = excludedKeywords
        target.excludedTitleKeywords = excludedTitleKeywords
        target.excludedCompanies = excludedCompanies
        target.matchWithAi = matchWithAi
        target.customPrompt = customPrompt
    }

    companion object {
        const val MAX_CUSTOM_PROMPT_LENGTH = 500
    }
}
