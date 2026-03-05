package com.mshykhov.jobhunter.api.rest.settings.dto

import com.mshykhov.jobhunter.application.outreach.OutreachSourceConfig

data class OutreachSourceConfigRequest(
    val coverLetterEnabled: Boolean = false,
    val recruiterMessageEnabled: Boolean = false,
    val coverLetterPrompt: String? = null,
    val recruiterMessagePrompt: String? = null,
) {
    fun toSourceConfig(): OutreachSourceConfig =
        OutreachSourceConfig(
            coverLetterEnabled = coverLetterEnabled,
            recruiterMessageEnabled = recruiterMessageEnabled,
            coverLetterPrompt = coverLetterPrompt,
            recruiterMessagePrompt = recruiterMessagePrompt,
        )
}
