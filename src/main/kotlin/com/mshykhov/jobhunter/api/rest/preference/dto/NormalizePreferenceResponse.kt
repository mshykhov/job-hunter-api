package com.mshykhov.jobhunter.api.rest.preference.dto

import com.mshykhov.jobhunter.application.ai.dto.NormalizedPreferences
import com.mshykhov.jobhunter.application.job.JobSource

data class NormalizePreferenceResponse(
    val rawInput: String,
    val categories: List<String>,
    val seniorityLevels: List<String>,
    val keywords: List<String>,
    val excludedKeywords: List<String>,
    val locations: List<String>,
    val remoteOnly: Boolean,
    val disabledSources: List<JobSource>,
) {
    companion object {
        fun from(
            rawInput: String,
            result: NormalizedPreferences,
        ): NormalizePreferenceResponse =
            NormalizePreferenceResponse(
                rawInput = rawInput,
                categories = result.categories,
                seniorityLevels = result.seniorityLevels,
                keywords = result.keywords,
                excludedKeywords = result.excludedKeywords,
                locations = result.locations,
                remoteOnly = result.remoteOnly,
                disabledSources = result.disabledSources,
            )
    }
}
