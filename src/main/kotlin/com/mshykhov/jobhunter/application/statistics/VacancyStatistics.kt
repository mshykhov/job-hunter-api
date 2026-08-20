package com.mshykhov.jobhunter.application.statistics

import com.mshykhov.jobhunter.application.job.JobSource
import com.mshykhov.jobhunter.application.user.UserFacade
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant

data class VacancyStatisticsQuery(val from: Instant?, val to: Instant?, val bucket: StatisticsBucket, val sources: List<JobSource>?)
data class VacancyStatisticsPoint(
    val start: Instant,
    val allVacancies: Long,
    val coldRejected: Long,
    val notFullyRemote: Long,
    val aiScored: Long,
    val legacyRejectedUnknown: Long,
    val medianScore: Double?,
)
data class VacancyStatisticsResult(
    val from: Instant,
    val to: Instant,
    val bucket: StatisticsBucket,
    val exactSince: Instant?,
    val sourceCoverageSince: Instant?,
    val points: List<VacancyStatisticsPoint>,
)

@Service
class VacancyStatisticsService(private val jdbcTemplate: NamedParameterJdbcTemplate, private val userFacade: UserFacade, private val clock: Clock) {
    fun query(userId: String, query: VacancyStatisticsQuery): VacancyStatisticsResult {
        val to = query.to ?: Instant.now(clock)
        val from = query.from ?: to.minusSeconds(DEFAULT_RANGE_SECONDS)
        require(!from.isAfter(to)) { "from must not be after to" }
        val starts =
            generateSequence(query.bucket.startOf(from)) { query.bucket.next(it) }
                .takeWhile { it.isBefore(to) }
                .toList()
        require(starts.size <= MAX_POINTS) { "Requested range produces more than $MAX_POINTS points" }
        val sourceNames = query.sources?.map { it.name }?.toTypedArray()
        val user =
            userFacade.findByAuth0Sub(userId)
                ?: return VacancyStatisticsResult(
                    from,
                    to,
                    query.bucket,
                    null,
                    null,
                    starts.map { VacancyStatisticsPoint(it, 0, 0, 0, 0, 0, null) },
                )
        val rows = jdbcTemplate.query(
            """
            SELECT date_trunc('${query.bucket.postgresUnit()}', vacancy_seen_at AT TIME ZONE 'UTC') AT TIME ZONE 'UTC' AS bucket_start,
                   count(DISTINCT group_id) AS all_vacancies,
                   count(DISTINCT group_id) FILTER (WHERE outcome = 'COLD_REJECTED') AS cold_rejected,
                   count(DISTINCT group_id) FILTER (WHERE outcome = 'AI_REJECTED_REMOTE') AS not_fully_remote,
                   count(DISTINCT group_id) FILTER (WHERE outcome = 'AI_SCORED') AS ai_scored,
                   count(DISTINCT group_id) FILTER (WHERE outcome = 'LEGACY_REJECTED_UNKNOWN') AS legacy_rejected_unknown,
                   percentile_cont(0.5) WITHIN GROUP (ORDER BY ai_score) FILTER (WHERE outcome = 'AI_SCORED' AND ai_score IS NOT NULL) AS median_score
            FROM user_job_group_decisions
            WHERE user_id = :userId AND vacancy_seen_at >= :from AND vacancy_seen_at < :to
              AND (:sourcesEmpty OR sources && CAST(:sources AS varchar[]))
            GROUP BY bucket_start ORDER BY bucket_start
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("userId", user.id)
                .addValue("from", from)
                .addValue("to", to)
                .addValue("sources", sourceNames)
                .addValue("sourcesEmpty", sourceNames.isNullOrEmpty()),
        ) { rs, _ ->
            VacancyStatisticsPoint(
                rs.getTimestamp("bucket_start").toInstant(),
                rs.getLong("all_vacancies"),
                rs.getLong("cold_rejected"),
                rs.getLong("not_fully_remote"),
                rs.getLong("ai_scored"),
                rs.getLong("legacy_rejected_unknown"),
                rs.getDouble("median_score").takeUnless {
                    rs.wasNull()
                },
            )
        }.associateBy { it.start }
        return VacancyStatisticsResult(
            from, to, query.bucket, null, null,
            starts.map { start ->
                rows[start]
                    ?: VacancyStatisticsPoint(start, 0, 0, 0, 0, 0, null)
            },
        )
    }

    companion object {
        private const val DEFAULT_RANGE_SECONDS = 30L * 24 * 60 * 60
        private const val MAX_POINTS = 400
    }
}
