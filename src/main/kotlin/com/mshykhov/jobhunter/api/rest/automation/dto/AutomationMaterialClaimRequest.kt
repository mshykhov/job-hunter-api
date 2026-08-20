package com.mshykhov.jobhunter.api.rest.automation.dto

import jakarta.validation.constraints.NotBlank

data class AutomationMaterialClaimRequest(@field:NotBlank val workerId: String)
