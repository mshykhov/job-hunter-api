package com.mshykhov.jobhunter.infrastructure.metrics

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class AutomationWorkflowMetrics(meterRegistry: MeterRegistry, reader: AutomationWorkflowMetricReader) {
    init {
        Gauge.builder(QUEUED_METRIC, reader) { it.read().queued.toDouble() }.register(meterRegistry)
        Gauge.builder(ACTIVE_METRIC, reader) { it.read().active.toDouble() }.register(meterRegistry)
        Gauge.builder(FAILED_METRIC, reader) { it.read().failed.toDouble() }.register(meterRegistry)
        Gauge.builder(RETRIES_METRIC, reader) { it.read().retries.toDouble() }.register(meterRegistry)
        Gauge.builder(OLDEST_ACTIONABLE_METRIC, reader) { it.read().oldestActionableEpochSeconds.toDouble() }
            .baseUnit("timestamp_seconds")
            .register(meterRegistry)
    }

    companion object {
        const val QUEUED_METRIC = "jobhunter.automation.workflow.queued"
        const val ACTIVE_METRIC = "jobhunter.automation.workflow.active"
        const val FAILED_METRIC = "jobhunter.automation.workflow.failed"
        const val RETRIES_METRIC = "jobhunter.automation.workflow.retries"
        const val OLDEST_ACTIONABLE_METRIC = "jobhunter.automation.workflow.oldest.actionable"
    }
}
