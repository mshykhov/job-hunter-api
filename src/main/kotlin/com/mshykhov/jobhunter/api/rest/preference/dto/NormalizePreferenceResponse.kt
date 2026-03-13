package com.mshykhov.jobhunter.api.rest.preference.dto

import com.mshykhov.jobhunter.application.ai.dto.NormalizedPreferences
import com.mshykhov.jobhunter.application.job.JobSource

data class NormalizePreferenceResponse(
    val categories: List<String>,
    val excludedKeywords: List<String>,
    val locations: List<String>,
    val remoteOnly: Boolean,
    val disabledSources: List<JobSource>,
) {
    companion object {
        fun from(result: NormalizedPreferences): NormalizePreferenceResponse =
            NormalizePreferenceResponse(
                categories = result.categories,
                excludedKeywords = result.excludedKeywords,
                locations = result.locations,
                remoteOnly = result.remoteOnly,
                disabledSources = result.disabledSources,
            )
    }
}
