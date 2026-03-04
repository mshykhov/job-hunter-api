package com.mshykhov.jobhunter.api.rest.job.dto

import com.mshykhov.jobhunter.application.job.JobSource

data class JobSourceResponse(
    val id: JobSource,
    val displayName: String,
) {
    companion object {
        fun from(source: JobSource): JobSourceResponse =
            JobSourceResponse(
                id = source,
                displayName = source.displayName,
            )
    }
}
