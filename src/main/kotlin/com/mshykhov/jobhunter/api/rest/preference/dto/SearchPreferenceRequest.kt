package com.mshykhov.jobhunter.api.rest.preference.dto

data class SearchPreferenceRequest(
    val rawInput: String? = null,
    val categories: List<String> = emptyList(),
    val seniorityLevels: List<String> = emptyList(),
    val locations: List<String> = emptyList(),
    val remoteOnly: Boolean = false,
)
