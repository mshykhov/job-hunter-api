package com.mshykhov.jobhunter.api.rest.settings.dto

import com.mshykhov.jobhunter.application.outreach.OutreachSourceConfig

data class OutreachSourceConfigResponse(
    val coverLetterEnabled: Boolean,
    val recruiterMessageEnabled: Boolean,
    val coverLetterPrompt: String?,
    val recruiterMessagePrompt: String?,
) {
    companion object {
        fun from(config: OutreachSourceConfig): OutreachSourceConfigResponse =
            OutreachSourceConfigResponse(
                coverLetterEnabled = config.coverLetterEnabled,
                recruiterMessageEnabled = config.recruiterMessageEnabled,
                coverLetterPrompt = config.coverLetterPrompt,
                recruiterMessagePrompt = config.recruiterMessagePrompt,
            )
    }
}
