package com.mshykhov.jobhunter.infrastructure.metrics

import com.mshykhov.jobhunter.application.automation.AutomationComponent
import com.mshykhov.jobhunter.application.automation.AutomationComponentSnapshot
import com.mshykhov.jobhunter.application.automation.AutomationProbeSnapshot
import com.mshykhov.jobhunter.application.automation.AutomationReason
import com.mshykhov.jobhunter.application.automation.AutomationState
import com.mshykhov.jobhunter.application.automation.ProbeOutcome
import com.mshykhov.jobhunter.application.automation.ProbeType
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AutomationMetricsTest {
    private val registry = SimpleMeterRegistry()
    private val metrics = AutomationMetrics(registry)

    @Test
    fun `registers exact bounded metric families`() {
        val names = registry.meters.map { it.id.name }.toSet()

        assertEquals(
            setOf(
                AutomationMetrics.ENABLED_METRIC,
                AutomationMetrics.LAST_HEARTBEAT_METRIC,
                AutomationMetrics.COMPONENT_STATE_METRIC,
                AutomationMetrics.PROBE_DURATION_METRIC,
                AutomationMetrics.PROBE_CONSECUTIVE_FAILURES_METRIC,
                AutomationMetrics.PROBE_LAST_SUCCESS_METRIC,
            ),
            names,
        )
        assertEquals(
            AutomationComponent.entries.map { it.name }.toSet(),
            registry.meters.filter { it.id.name == AutomationMetrics.COMPONENT_STATE_METRIC }
                .mapNotNull { it.id.getTag("component") }
                .toSet(),
        )
        assertEquals(
            AutomationState.entries.map { it.name }.toSet(),
            registry.meters.filter { it.id.name == AutomationMetrics.COMPONENT_STATE_METRIC }
                .mapNotNull { it.id.getTag("state") }
                .toSet(),
        )
        assertEquals(
            ProbeType.entries.map { it.name }.toSet(),
            registry.meters.filter { it.id.getTag("probe") != null }
                .mapNotNull { it.id.getTag("probe") }
                .toSet(),
        )
    }

    @Test
    fun `restores one-hot state and probe gauges from durable snapshot`() {
        val checkedAt = Instant.parse("2026-08-18T08:00:00Z")
        metrics.restore(
            enabled = true,
            lastHeartbeatAt = checkedAt,
            components =
            mapOf(
                AutomationComponent.CODEX to
                    AutomationComponentSnapshot(
                        state = AutomationState.READY,
                        reason = AutomationReason.NONE,
                        checkedAt = checkedAt,
                        probeVersion = "0.1.0",
                    ),
            ),
            probes =
            mapOf(
                ProbeType.CODEX to
                    AutomationProbeSnapshot(
                        outcome = ProbeOutcome.SUCCESS,
                        reason = AutomationReason.NONE,
                        durationMillis = 1250,
                        consecutiveFailures = 0,
                        lastSuccessAt = checkedAt,
                    ),
            ),
        )

        assertEquals(1.0, registry.get(AutomationMetrics.ENABLED_METRIC).gauge().value())
        assertEquals(checkedAt.epochSecond.toDouble(), registry.get(AutomationMetrics.LAST_HEARTBEAT_METRIC).gauge().value())
        assertEquals(
            1.0,
            registry.get(AutomationMetrics.COMPONENT_STATE_METRIC).tags("component", "CODEX", "state", "READY").gauge().value(),
        )
        assertEquals(
            0.0,
            registry.get(AutomationMetrics.COMPONENT_STATE_METRIC).tags("component", "CODEX", "state", "DEGRADED").gauge().value(),
        )
        assertEquals(1.25, registry.get(AutomationMetrics.PROBE_DURATION_METRIC).tag("probe", "CODEX").gauge().value())
        assertEquals(0.0, registry.get(AutomationMetrics.PROBE_CONSECUTIVE_FAILURES_METRIC).tag("probe", "CODEX").gauge().value())
        assertEquals(
            checkedAt.epochSecond.toDouble(),
            registry.get(AutomationMetrics.PROBE_LAST_SUCCESS_METRIC).tag("probe", "CODEX").gauge().value(),
        )
    }

    @Test
    fun `records counters with allowlisted tags and collapses unknown reason`() {
        metrics.recordProbe(ProbeType.CODEX, ProbeOutcome.FAILURE, "vendor-specific-error")
        metrics.recordCodexTokens(input = 17, output = 5)

        assertEquals(
            1.0,
            registry.get(AutomationMetrics.PROBE_METRIC)
                .tags("probe", "CODEX", "outcome", "FAILURE", "reason", "OTHER")
                .counter()
                .count(),
        )
        assertEquals(
            17.0,
            registry.get(AutomationMetrics.CODEX_TOKENS_METRIC).tag("direction", "INPUT").counter().count(),
        )
        assertEquals(
            5.0,
            registry.get(AutomationMetrics.CODEX_TOKENS_METRIC).tag("direction", "OUTPUT").counter().count(),
        )
        assertTrue(registry.meters.none { it.id.getTag("reason") == "vendor-specific-error" })
    }

    @Test
    fun `renders documented prometheus names and tags`() {
        val prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        val prometheusMetrics = AutomationMetrics(prometheusRegistry)
        val checkedAt = Instant.parse("2026-08-18T08:00:00Z")
        prometheusMetrics.restore(
            enabled = true,
            lastHeartbeatAt = checkedAt,
            components =
            mapOf(
                AutomationComponent.CODEX to
                    AutomationComponentSnapshot(
                        state = AutomationState.READY,
                        reason = AutomationReason.NONE,
                        checkedAt = checkedAt,
                        probeVersion = "0.1.0",
                    ),
            ),
            probes =
            mapOf(
                ProbeType.CODEX to
                    AutomationProbeSnapshot(
                        outcome = ProbeOutcome.SUCCESS,
                        reason = AutomationReason.NONE,
                        durationMillis = 1250,
                        consecutiveFailures = 0,
                        lastSuccessAt = checkedAt,
                    ),
            ),
        )
        prometheusMetrics.recordProbe(ProbeType.CODEX, ProbeOutcome.SUCCESS, AutomationReason.NONE.name)
        prometheusMetrics.recordCodexTokens(input = 17, output = 5)

        val scrape = prometheusRegistry.scrape()

        assertTrue(scrape.contains("jobhunter_automation_enabled 1.0"), scrape)
        assertTrue(scrape.contains("jobhunter_automation_runner_last_heartbeat_timestamp_seconds"), scrape)
        assertTrue(scrape.contains("jobhunter_automation_component_state{component=\"CODEX\",state=\"READY\"} 1.0"), scrape)
        assertTrue(scrape.contains("jobhunter_automation_probe_total{outcome=\"SUCCESS\",probe=\"CODEX\",reason=\"NONE\"} 1.0"), scrape)
        assertTrue(scrape.contains("jobhunter_automation_probe_duration_seconds{probe=\"CODEX\"} 1.25"), scrape)
        assertTrue(scrape.contains("jobhunter_automation_probe_consecutive_failures{probe=\"CODEX\"} 0.0"), scrape)
        assertTrue(scrape.contains("jobhunter_automation_probe_last_success_timestamp_seconds{probe=\"CODEX\"}"), scrape)
        assertTrue(scrape.contains("jobhunter_automation_codex_tokens_total{direction=\"INPUT\"} 17.0"), scrape)
    }
}
