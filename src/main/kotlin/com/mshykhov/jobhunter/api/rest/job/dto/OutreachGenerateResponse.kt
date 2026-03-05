package com.mshykhov.jobhunter.api.rest.job.dto

import com.mshykhov.jobhunter.application.job.JobEntity
import com.mshykhov.jobhunter.application.job.JobSource
import java.util.UUID

data class CoverLetterResponse(
    val coverLetter: String,
    val job: OutreachJobInfo,
) {
    companion object {
        fun of(
            coverLetter: String,
            job: JobEntity,
        ): CoverLetterResponse = CoverLetterResponse(coverLetter, OutreachJobInfo.from(job))
    }
}

data class RecruiterMessageResponse(
    val recruiterMessage: String,
    val job: OutreachJobInfo,
) {
    companion object {
        fun of(
            recruiterMessage: String,
            job: JobEntity,
        ): RecruiterMessageResponse = RecruiterMessageResponse(recruiterMessage, OutreachJobInfo.from(job))
    }
}

data class OutreachJobInfo(
    val id: UUID,
    val title: String,
    val company: String?,
    val url: String,
    val source: JobSource,
) {
    companion object {
        fun from(job: JobEntity): OutreachJobInfo =
            OutreachJobInfo(
                id = job.id,
                title = job.title,
                company = job.company,
                url = job.url,
                source = job.source,
            )
    }
}
