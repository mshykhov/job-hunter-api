package com.mshykhov.jobhunter.infrastructure.metrics

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.Instant

data class AutomationWorkflowMetricSnapshot(val queued: Long, val active: Long, val failed: Long, val retries: Long, val oldestActionableEpochSeconds: Long)

interface AutomationWorkflowMetricReader {
    fun read(): AutomationWorkflowMetricSnapshot
}

@Component
class JdbcAutomationWorkflowMetricReader(private val jdbcTemplate: JdbcTemplate, private val clock: Clock) : AutomationWorkflowMetricReader {
    @Volatile
    private var cached = AutomationWorkflowMetricSnapshot(0, 0, 0, 0, 0)

    @Volatile
    private var refreshAfter = Instant.MIN

    override fun read(): AutomationWorkflowMetricSnapshot {
        val now = Instant.now(clock)
        if (now.isBefore(refreshAfter)) return cached
        return synchronized(this) {
            if (now.isBefore(refreshAfter)) return@synchronized cached
            cached =
                requireNotNull(
                    jdbcTemplate.queryForObject(
                        """
                        SELECT
                            COUNT(*) FILTER (WHERE w.status = 'QUEUED' AND r.status IN ('QUEUED', 'RUNNING')) AS queued,
                            COUNT(*) FILTER (WHERE w.status = 'LEASED') AS active,
                            COUNT(*) FILTER (WHERE w.status = 'FAILED') AS failed,
                            COALESCE(SUM(GREATEST(w.attempt_count - 1, 0)), 0) AS retries,
                            COALESCE(EXTRACT(EPOCH FROM MIN(w.updated_at) FILTER (
                                WHERE w.status IN ('QUEUED', 'LEASED') AND r.status IN ('QUEUED', 'RUNNING')
                            ))::BIGINT, 0) AS oldest_actionable
                        FROM automation_work_items w
                        JOIN automation_workflow_runs r ON r.id = w.run_id
                        """.trimIndent(),
                    ) { result, _ ->
                        AutomationWorkflowMetricSnapshot(
                            queued = result.getLong("queued"),
                            active = result.getLong("active"),
                            failed = result.getLong("failed"),
                            retries = result.getLong("retries"),
                            oldestActionableEpochSeconds = result.getLong("oldest_actionable"),
                        )
                    },
                )
            refreshAfter = now.plus(CACHE_DURATION)
            cached
        }
    }

    private companion object {
        val CACHE_DURATION: Duration = Duration.ofSeconds(10)
    }
}
