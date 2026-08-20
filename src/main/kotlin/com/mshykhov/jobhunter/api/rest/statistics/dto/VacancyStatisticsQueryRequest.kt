package com.mshykhov.jobhunter.api.rest.statistics.dto

import com.mshykhov.jobhunter.application.job.JobSource
import com.mshykhov.jobhunter.application.statistics.StatisticsBucket
import com.mshykhov.jobhunter.application.statistics.VacancyStatisticsQuery
import java.time.Instant

data class VacancyStatisticsQueryRequest(
    val from: Instant? = null,
    val to: Instant? = null,
    val bucket: StatisticsBucket = StatisticsBucket.DAY,
    val sources: List<JobSource>? = null,
) {
    fun toQuery() = VacancyStatisticsQuery(from, to, bucket, sources)
}
