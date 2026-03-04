package com.mshykhov.jobhunter.api.rest.preference.dto

import com.mshykhov.jobhunter.application.preference.SearchPreferences

data class SearchPreferenceResponse(
    val rawInput: String?,
    val categories: List<String>,
    val seniorityLevels: List<String>,
    val locations: List<String>,
    val remoteOnly: Boolean,
) {
    companion object {
        fun from(search: SearchPreferences): SearchPreferenceResponse =
            SearchPreferenceResponse(
                rawInput = search.rawInput,
                categories = search.categories,
                seniorityLevels = search.seniorityLevels,
                locations = search.locations,
                remoteOnly = search.remoteOnly,
            )
    }
}
