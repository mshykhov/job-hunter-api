package com.mshykhov.jobhunter.api.rest.job.dto

import com.mshykhov.jobhunter.application.userjob.UserJobStatus
import jakarta.validation.constraints.NotEmpty
import java.util.UUID

data class BulkUpdateStatusRequest(
    @field:NotEmpty
    val jobIds: List<UUID>,
    val status: UserJobStatus,
)
