package com.mshykhov.jobhunter.api.rest.settings.dto

import com.mshykhov.jobhunter.application.job.JobSource
import jakarta.validation.constraints.NotNull

data class OutreachTestRequest(
    @field:NotNull
    val source: JobSource,
)
