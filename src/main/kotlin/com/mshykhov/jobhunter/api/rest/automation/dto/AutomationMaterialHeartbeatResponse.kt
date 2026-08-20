package com.mshykhov.jobhunter.api.rest.automation.dto

import java.time.Instant

data class AutomationMaterialHeartbeatResponse(val leaseExpiresAt: Instant)
