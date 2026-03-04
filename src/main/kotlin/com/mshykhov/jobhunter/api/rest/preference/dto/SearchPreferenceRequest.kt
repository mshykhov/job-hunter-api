package com.mshykhov.jobhunter.api.rest.preference.dto

import com.mshykhov.jobhunter.application.job.JobSource

data class SearchPreferenceRequest(
    val rawInput: String? = null,
    val categories: List<String> = emptyList(),
    val locations: List<String> = emptyList(),
    val remoteOnly: Boolean = false,
    val disabledSources: List<JobSource> = emptyList(),
)
