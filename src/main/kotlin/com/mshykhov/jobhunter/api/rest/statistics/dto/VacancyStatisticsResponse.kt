package com.mshykhov.jobhunter.api.rest.statistics.dto

import com.mshykhov.jobhunter.application.statistics.VacancyStatisticsResult
import java.time.Instant

data class VacancyStatisticsResponse(
    val from: Instant,
    val to: Instant,
    val bucket: String,
    val exactSince: Instant?,
    val sourceCoverageSince: Instant?,
    val points: List<VacancyStatisticsPointResponse>,
) {
    companion object {
        fun from(
            value: VacancyStatisticsResult,
        ) = VacancyStatisticsResponse(
            value.from, value.to, value.bucket.name, value.exactSince, value.sourceCoverageSince,
            value.points.map {
                VacancyStatisticsPointResponse.from(it)
            },
        )
    }
}
data class VacancyStatisticsPointResponse(
    val start: Instant,
    val allVacancies: Long,
    val coldRejected: Long,
    val notFullyRemote: Long,
    val aiScored: Long,
    val legacyRejectedUnknown: Long,
    val medianScore: Double?,
) {
    companion object {
        fun from(
            value: com.mshykhov.jobhunter.application.statistics.VacancyStatisticsPoint,
        ) = VacancyStatisticsPointResponse(
            value.start,
            value.allVacancies,
            value.coldRejected,
            value.notFullyRemote,
            value.aiScored,
            value.legacyRejectedUnknown,
            value.medianScore,
        )
    }
}
