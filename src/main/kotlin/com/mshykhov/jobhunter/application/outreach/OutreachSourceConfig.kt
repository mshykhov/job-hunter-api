package com.mshykhov.jobhunter.application.outreach

data class OutreachSourceConfig(
    val coverLetterEnabled: Boolean = false,
    val recruiterMessageEnabled: Boolean = false,
    val coverLetterPrompt: String? = null,
    val recruiterMessagePrompt: String? = null,
)
