package com.mshykhov.jobhunter.api.rest.preference.dto

import jakarta.validation.constraints.NotBlank

data class AboutRequest(
    @field:NotBlank
    val about: String,
)
