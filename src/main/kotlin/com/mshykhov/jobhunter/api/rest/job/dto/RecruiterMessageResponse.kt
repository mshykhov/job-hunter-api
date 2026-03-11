package com.mshykhov.jobhunter.api.rest.job.dto

import com.mshykhov.jobhunter.application.job.JobEntity

data class RecruiterMessageResponse(val recruiterMessage: String, val job: OutreachJobInfo) {
    companion object {
        fun of(
            recruiterMessage: String,
            job: JobEntity,
        ): RecruiterMessageResponse = RecruiterMessageResponse(recruiterMessage, OutreachJobInfo.from(job))
    }
}
