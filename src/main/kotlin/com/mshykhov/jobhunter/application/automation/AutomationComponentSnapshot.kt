package com.mshykhov.jobhunter.application.automation

import java.time.Instant

data class AutomationComponentSnapshot(val state: AutomationState, val reason: AutomationReason, val checkedAt: Instant, val probeVersion: String)
