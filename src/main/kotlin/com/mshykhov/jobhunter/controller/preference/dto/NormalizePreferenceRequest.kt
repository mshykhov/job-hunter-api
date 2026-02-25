package com.mshykhov.jobhunter.controller.preference.dto

import jakarta.validation.constraints.NotBlank

data class NormalizePreferenceRequest(
    @field:NotBlank
    val rawInput: String,
)
