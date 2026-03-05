package com.mshykhov.jobhunter.api.rest.settings.dto

import com.mshykhov.jobhunter.application.ai.OutreachGenerator
import com.mshykhov.jobhunter.application.job.JobSource
import com.mshykhov.jobhunter.application.outreach.OutreachSettingsEntity

data class OutreachSettingsResponse(
    val coverLetterPrompt: String?,
    val recruiterMessagePrompt: String?,
    val sourceConfig: Map<JobSource, OutreachSourceConfigResponse>,
    val defaultCoverLetterPrompt: String,
    val defaultRecruiterMessagePrompt: String,
) {
    companion object {
        fun from(entity: OutreachSettingsEntity?): OutreachSettingsResponse {
            val sourceConfig =
                entity
                    ?.sourceConfig
                    ?.mapKeys { JobSource.valueOf(it.key) }
                    ?.mapValues { OutreachSourceConfigResponse.from(it.value) }
                    ?: emptyMap()
            return OutreachSettingsResponse(
                coverLetterPrompt = entity?.coverLetterPrompt,
                recruiterMessagePrompt = entity?.recruiterMessagePrompt,
                sourceConfig = sourceConfig,
                defaultCoverLetterPrompt = OutreachGenerator.DEFAULT_COVER_LETTER_PROMPT,
                defaultRecruiterMessagePrompt = OutreachGenerator.DEFAULT_RECRUITER_MESSAGE_PROMPT,
            )
        }
    }
}
