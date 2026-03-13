package com.mshykhov.jobhunter.api.rest.preference.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class AboutRequest(
    @field:NotBlank
    @field:Size(max = MAX_ABOUT_LENGTH, message = "About must not exceed $MAX_ABOUT_LENGTH characters")
    val about: String,
) {
    companion object {
        const val MAX_ABOUT_LENGTH = 3000
    }
}
