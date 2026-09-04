package com.mshykhov.jobhunter.infrastructure.metrics

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AutomationWorkflowMetricsTest {
    @Test
    fun `publishes durable workflow gauges without identifier labels`() {
        val registry = SimpleMeterRegistry()
        val reader =
            object : AutomationWorkflowMetricReader {
                override fun read() = AutomationWorkflowMetricSnapshot(queued = 2, active = 1, failed = 3, retries = 4, oldestActionableEpochSeconds = 42)
            }

        AutomationWorkflowMetrics(registry, reader)

        assertEquals(2.0, registry.get(AutomationWorkflowMetrics.QUEUED_METRIC).gauge().value())
        assertEquals(1.0, registry.get(AutomationWorkflowMetrics.ACTIVE_METRIC).gauge().value())
        assertEquals(3.0, registry.get(AutomationWorkflowMetrics.FAILED_METRIC).gauge().value())
        assertEquals(4.0, registry.get(AutomationWorkflowMetrics.RETRIES_METRIC).gauge().value())
        assertEquals(42.0, registry.get(AutomationWorkflowMetrics.OLDEST_ACTIONABLE_METRIC).gauge().value())
        assertEquals(emptySet(), registry.meters.flatMap { it.id.tags }.map { it.key }.toSet())
    }
}
