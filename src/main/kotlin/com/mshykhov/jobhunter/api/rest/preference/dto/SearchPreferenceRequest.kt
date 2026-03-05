package com.mshykhov.jobhunter.api.rest.preference.dto

import com.mshykhov.jobhunter.application.job.JobSource
import com.mshykhov.jobhunter.application.preference.SearchPreferences

data class SearchPreferenceRequest(
    val categories: List<String> = emptyList(),
    val locations: List<String> = emptyList(),
    val remoteOnly: Boolean = false,
    val disabledSources: List<JobSource> = emptyList(),
) {
    fun applyTo(target: SearchPreferences) {
        target.categories = categories
        target.locations = locations
        target.remoteOnly = remoteOnly
        target.disabledSources = disabledSources
    }
}
