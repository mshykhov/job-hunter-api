package com.mshykhov.jobhunter.api.rest.preference.dto

import jakarta.validation.constraints.NotBlank

data class NormalizePreferenceRequest(
    @field:NotBlank
    val rawInput: String,
)
