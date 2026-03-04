package com.mshykhov.jobhunter.api.rest.settings.dto

data class AiProviderResponse(
    val id: String,
    val name: String,
    val recommended: Boolean,
    val models: List<AiModelResponse>,
)
