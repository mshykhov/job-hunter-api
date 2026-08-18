package com.mshykhov.jobhunter.api.rest.automation.dto

import com.mshykhov.jobhunter.application.automation.AutomationComponent
import com.mshykhov.jobhunter.application.automation.AutomationComponentSnapshot
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.time.Instant
import java.util.UUID

data class AutomationHeartbeatRequest(
    @field:Min(1)
    val generation: Long,
    @field:Min(1)
    val sequence: Long,
    val idempotencyKey: UUID,
    val sentAt: Instant,
    @field:NotBlank
    val launcherVersion: String,
    val components: Map<AutomationComponent, AutomationComponentSnapshot>,
    val lastPreflightSuccessAt: Instant? = null,
    val lastCodexSuccessAt: Instant? = null,
    @field:Min(0)
    val codexInputTokens: Long = 0,
    @field:Min(0)
    val codexOutputTokens: Long = 0,
)
