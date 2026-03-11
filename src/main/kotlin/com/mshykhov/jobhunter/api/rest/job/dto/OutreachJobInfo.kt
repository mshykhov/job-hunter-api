package com.mshykhov.jobhunter.api.rest.job.dto

import com.mshykhov.jobhunter.application.job.JobEntity
import com.mshykhov.jobhunter.application.job.JobSource
import java.util.UUID

data class OutreachJobInfo(val id: UUID, val title: String, val company: String?, val url: String, val source: JobSource) {
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
