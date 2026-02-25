package com.mshykhov.jobhunter.api.rest.job.dto

import com.mshykhov.jobhunter.application.userjob.UserJobStatus

data class UpdateJobStatusRequest(
    val status: UserJobStatus,
)
