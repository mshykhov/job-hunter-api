package com.mshykhov.jobhunter.api.rest.automation.dto

import com.mshykhov.jobhunter.application.automation.AutomationState

data class AutomationHeartbeatResponse(val generation: Long, val acceptedSequence: Long, val overallState: AutomationState)
