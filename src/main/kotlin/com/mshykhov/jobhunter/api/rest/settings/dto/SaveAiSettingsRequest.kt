package com.mshykhov.jobhunter.api.rest.settings.dto

import jakarta.validation.constraints.NotBlank

data class SaveAiSettingsRequest(
    @field:NotBlank
    val apiKey: String,
    @field:NotBlank
    val modelId: String,
)
