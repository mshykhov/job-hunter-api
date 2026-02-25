package com.mshykhov.jobhunter.controller.job.dto

import com.mshykhov.jobhunter.persistence.model.UserJobStatus

data class UpdateJobStatusRequest(
    val status: UserJobStatus,
)
