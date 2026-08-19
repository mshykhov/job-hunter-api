package com.mshykhov.jobhunter.api.rest.automation.dto

data class AutomationSessionResponse(
    val runnerKey: String,
    val generation: Long,
    val heartbeatIntervalSeconds: Long,
    val preflightIntervalSeconds: Long,
    val codexCanaryIntervalSeconds: Long,
)
