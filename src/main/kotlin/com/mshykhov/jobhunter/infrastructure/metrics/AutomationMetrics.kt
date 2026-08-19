package com.mshykhov.jobhunter.infrastructure.metrics

import com.mshykhov.jobhunter.application.automation.AutomationComponent
import com.mshykhov.jobhunter.application.automation.AutomationComponentSnapshot
import com.mshykhov.jobhunter.application.automation.AutomationProbeSnapshot
import com.mshykhov.jobhunter.application.automation.AutomationReason
import com.mshykhov.jobhunter.application.automation.AutomationState
import com.mshykhov.jobhunter.application.automation.ProbeOutcome
import com.mshykhov.jobhunter.application.automation.ProbeType
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

private val logger = KotlinLogging.logger {}

@Component
class AutomationMetrics(private val meterRegistry: MeterRegistry) {
    private val enabled = AtomicInteger(0)
    private val lastHeartbeat = AtomicLong(0)
    private val componentStates =
        AutomationComponent.entries.flatMap { component ->
            AutomationState.entries.map { state -> (component to state) to AtomicInteger(initialStateValue(state)) }
        }.toMap()
    private val probeDurations = ProbeType.entries.associateWith { AtomicReference(0.0) }
    private val probeConsecutiveFailures = ProbeType.entries.associateWith { AtomicInteger(0) }
    private val probeLastSuccess = ProbeType.entries.associateWith { AtomicLong(0) }

    init {
        Gauge.builder(ENABLED_METRIC, enabled) { it.get().toDouble() }.register(meterRegistry)
        Gauge.builder(LAST_HEARTBEAT_METRIC, lastHeartbeat) { it.get().toDouble() }
            .baseUnit(TIMESTAMP_SECONDS_UNIT)
            .register(meterRegistry)
        componentStates.forEach { (key, value) ->
            Gauge.builder(COMPONENT_STATE_METRIC, value) { it.get().toDouble() }
                .tags(COMPONENT_TAG, key.first.name, STATE_TAG, key.second.name)
                .register(meterRegistry)
        }
        ProbeType.entries.forEach { probe ->
            Gauge.builder(PROBE_DURATION_METRIC, probeDurations.getValue(probe)) { it.get() }
                .tag(PROBE_TAG, probe.name)
                .baseUnit(SECONDS_UNIT)
                .register(meterRegistry)
            Gauge.builder(PROBE_CONSECUTIVE_FAILURES_METRIC, probeConsecutiveFailures.getValue(probe)) {
                it.get().toDouble()
            }.tag(PROBE_TAG, probe.name).register(meterRegistry)
            Gauge.builder(PROBE_LAST_SUCCESS_METRIC, probeLastSuccess.getValue(probe)) { it.get().toDouble() }
                .tag(PROBE_TAG, probe.name)
                .baseUnit(TIMESTAMP_SECONDS_UNIT)
                .register(meterRegistry)
        }
    }

    fun setEnabled(value: Boolean) {
        enabled.set(if (value) 1 else 0)
    }

    fun restore(
        enabled: Boolean,
        lastHeartbeatAt: Instant?,
        components: Map<AutomationComponent, AutomationComponentSnapshot>,
        probes: Map<ProbeType, AutomationProbeSnapshot>,
    ) {
        setEnabled(enabled)
        lastHeartbeat.set(lastHeartbeatAt?.epochSecond ?: 0)
        AutomationComponent.entries.forEach { component ->
            val current = components[component]?.state ?: AutomationState.UNAVAILABLE
            AutomationState.entries.forEach { state ->
                componentStates.getValue(component to state).set(if (state == current) 1 else 0)
            }
        }
        ProbeType.entries.forEach { probe ->
            val snapshot = probes[probe]
            probeDurations.getValue(probe).set(snapshot?.durationMillis?.div(MILLIS_PER_SECOND) ?: 0.0)
            probeConsecutiveFailures.getValue(probe).set(snapshot?.consecutiveFailures ?: 0)
            probeLastSuccess.getValue(probe).set(snapshot?.lastSuccessAt?.epochSecond ?: 0)
        }
    }

    fun recordHeartbeat(
        lastHeartbeatAt: Instant,
        components: Map<AutomationComponent, AutomationComponentSnapshot>,
        probes: Map<ProbeType, AutomationProbeSnapshot>,
        codexInputTokens: Long,
        codexOutputTokens: Long,
    ) {
        restore(true, lastHeartbeatAt, components, probes)
        probes.forEach { (probe, snapshot) ->
            recordProbe(probe, snapshot.outcome, snapshot.reason.name)
        }
        recordCodexTokens(codexInputTokens, codexOutputTokens)
    }

    fun recordProbe(
        probe: ProbeType,
        outcome: ProbeOutcome,
        reason: String,
    ) {
        try {
            val reasonTag = AutomationReason.entries.firstOrNull { it.name == reason }?.name ?: AutomationReason.OTHER.name
            meterRegistry.counter(
                PROBE_METRIC,
                PROBE_TAG,
                probe.name,
                OUTCOME_TAG,
                outcome.name,
                REASON_TAG,
                reasonTag,
            ).increment()
        } catch (e: Exception) {
            logger.warn(e) { "Failed to record automation probe metric for $probe" }
        }
    }

    fun recordCodexTokens(
        input: Long,
        output: Long,
    ) {
        recordTokens(INPUT_DIRECTION, input)
        recordTokens(OUTPUT_DIRECTION, output)
    }

    private fun recordTokens(
        direction: String,
        count: Long,
    ) {
        try {
            meterRegistry.counter(CODEX_TOKENS_METRIC, DIRECTION_TAG, direction).increment(count.toDouble())
        } catch (e: Exception) {
            logger.warn(e) { "Failed to record Codex token metric for $direction" }
        }
    }

    companion object {
        const val ENABLED_METRIC = "jobhunter.automation.enabled"
        const val LAST_HEARTBEAT_METRIC = "jobhunter.automation.runner.last.heartbeat"
        const val COMPONENT_STATE_METRIC = "jobhunter.automation.component.state"
        const val PROBE_METRIC = "jobhunter.automation.probe"
        const val PROBE_DURATION_METRIC = "jobhunter.automation.probe.duration"
        const val PROBE_CONSECUTIVE_FAILURES_METRIC = "jobhunter.automation.probe.consecutive.failures"
        const val PROBE_LAST_SUCCESS_METRIC = "jobhunter.automation.probe.last.success"
        const val CODEX_TOKENS_METRIC = "jobhunter.automation.codex.tokens"

        private const val COMPONENT_TAG = "component"
        private const val STATE_TAG = "state"
        private const val PROBE_TAG = "probe"
        private const val OUTCOME_TAG = "outcome"
        private const val REASON_TAG = "reason"
        private const val DIRECTION_TAG = "direction"
        private const val INPUT_DIRECTION = "INPUT"
        private const val OUTPUT_DIRECTION = "OUTPUT"
        private const val SECONDS_UNIT = "seconds"
        private const val TIMESTAMP_SECONDS_UNIT = "timestamp_seconds"
        private const val MILLIS_PER_SECOND = 1000.0

        private fun initialStateValue(state: AutomationState): Int = if (state == AutomationState.UNAVAILABLE) 1 else 0
    }
}
