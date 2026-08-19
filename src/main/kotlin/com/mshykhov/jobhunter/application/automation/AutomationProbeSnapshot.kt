package com.mshykhov.jobhunter.application.automation

import jakarta.validation.constraints.Min
import java.time.Instant

data class AutomationProbeSnapshot(
    val outcome: ProbeOutcome,
    val reason: AutomationReason,
    @field:Min(0)
    val durationMillis: Long,
    @field:Min(0)
    val consecutiveFailures: Int,
    val lastSuccessAt: Instant? = null,
)
