package com.mshykhov.jobhunter.api.rest.automation.dto

import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class AutomationMaterialFailureRequest(val leaseToken: UUID, val retryable: Boolean, @field:NotBlank val reasonCode: String)
