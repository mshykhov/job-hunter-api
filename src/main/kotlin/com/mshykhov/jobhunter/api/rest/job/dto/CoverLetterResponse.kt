package com.mshykhov.jobhunter.api.rest.job.dto

import com.mshykhov.jobhunter.application.job.JobEntity

data class CoverLetterResponse(val coverLetter: String, val job: OutreachJobInfo) {
    companion object {
        fun of(
            coverLetter: String,
            job: JobEntity,
        ): CoverLetterResponse = CoverLetterResponse(coverLetter, OutreachJobInfo.from(job))
    }
}
