package com.mshykhov.jobhunter.api.rest.job.dto

import com.mshykhov.jobhunter.application.userjob.UserJobStatus

data class UserJobStatusResponse(val id: UserJobStatus, val displayName: String) {
    companion object {
        fun from(status: UserJobStatus): UserJobStatusResponse =
            UserJobStatusResponse(
                id = status,
                displayName = status.displayName,
            )
    }
}
