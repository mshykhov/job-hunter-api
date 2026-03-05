package com.mshykhov.jobhunter.api.rest.settings.dto

import com.mshykhov.jobhunter.application.job.JobSource
import com.mshykhov.jobhunter.application.outreach.OutreachSettingsEntity
import com.mshykhov.jobhunter.application.user.UserEntity

data class SaveOutreachSettingsRequest(
    val coverLetterPrompt: String? = null,
    val recruiterMessagePrompt: String? = null,
    val sourceConfig: Map<JobSource, OutreachSourceConfigRequest> = emptyMap(),
) {
    fun applyTo(target: OutreachSettingsEntity) {
        target.coverLetterPrompt = coverLetterPrompt
        target.recruiterMessagePrompt = recruiterMessagePrompt
        target.sourceConfig = sourceConfig.mapKeys { it.key.name }.mapValues { it.value.toSourceConfig() }
    }

    fun toEntity(user: UserEntity): OutreachSettingsEntity =
        OutreachSettingsEntity(
            user = user,
            coverLetterPrompt = coverLetterPrompt,
            recruiterMessagePrompt = recruiterMessagePrompt,
            sourceConfig = sourceConfig.mapKeys { it.key.name }.mapValues { it.value.toSourceConfig() },
        )
}
