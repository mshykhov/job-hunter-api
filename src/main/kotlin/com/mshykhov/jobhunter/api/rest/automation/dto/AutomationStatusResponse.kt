package com.mshykhov.jobhunter.api.rest.automation.dto

import com.mshykhov.jobhunter.application.automation.AutomationComponent
import com.mshykhov.jobhunter.application.automation.AutomationComponentSnapshot
import com.mshykhov.jobhunter.application.automation.AutomationReason
import com.mshykhov.jobhunter.application.automation.AutomationState
import java.time.Instant

data class AutomationStatusResponse(
    val enabled: Boolean,
    val state: AutomationState,
    val reason: AutomationReason,
    val components: Map<AutomationComponent, AutomationComponentSnapshot>,
    val launcherVersion: String?,
    val lastHeartbeatAt: Instant?,
    val lastPreflightSuccessAt: Instant?,
    val lastCodexSuccessAt: Instant?,
)
