package com.mshykhov.jobhunter.application.automation

import com.mshykhov.jobhunter.infrastructure.automation.AutomationProperties
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals

class AutomationHealthPolicyTest {
    private val now = Instant.parse("2026-08-18T08:00:00Z")
    private val policy = AutomationHealthPolicy(AutomationProperties())

    @Test
    fun `authentication required takes precedence`() {
        val components = readyComponents().toMutableMap()
        components[AutomationComponent.CODEX] = snapshot(AutomationState.AUTH_REQUIRED)

        assertEquals(AutomationState.AUTH_REQUIRED, policy.overallState(components, now))
    }

    @Test
    fun `stale required component is unavailable`() {
        val components = readyComponents().toMutableMap()
        components[AutomationComponent.CHROME] = snapshot(checkedAt = now.minus(Duration.ofMinutes(11)))

        assertEquals(AutomationState.UNAVAILABLE, policy.overallState(components, now))
    }

    @Test
    fun `fresh degraded component degrades overall state`() {
        val components = readyComponents().toMutableMap()
        components[AutomationComponent.PLAYWRIGHT] = snapshot(AutomationState.DEGRADED)

        assertEquals(AutomationState.DEGRADED, policy.overallState(components, now))
    }

    @Test
    fun `all fresh ready components are ready`() {
        assertEquals(AutomationState.READY, policy.overallState(readyComponents(), now))
    }

    @Test
    fun `missing required component is unavailable`() {
        assertEquals(AutomationState.UNAVAILABLE, policy.overallState(emptyMap(), now))
    }

    private fun readyComponents(): Map<AutomationComponent, AutomationComponentSnapshot> =
        AutomationComponent.entries.associateWith { snapshot() }

    private fun snapshot(
        state: AutomationState = AutomationState.READY,
        checkedAt: Instant = now,
    ): AutomationComponentSnapshot =
        AutomationComponentSnapshot(
            state = state,
            reason = if (state == AutomationState.READY) AutomationReason.NONE else AutomationReason.OTHER,
            checkedAt = checkedAt,
            probeVersion = "0.1.0",
        )
}
