package com.mshykhov.jobhunter.infrastructure.metrics

import com.mshykhov.jobhunter.application.automation.AutomationFacade
import com.mshykhov.jobhunter.infrastructure.automation.AutomationProperties
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class AutomationMetricsRestorer(private val facade: AutomationFacade, private val properties: AutomationProperties, private val metrics: AutomationMetrics) {
    @EventListener(ApplicationReadyEvent::class)
    fun restore() {
        val delegation = facade.findDelegation(properties.ownerIssuer, properties.ownerSubject)
        val runner = delegation?.let { facade.findRunner(it.id) }
        metrics.restore(
            enabled = delegation?.healthReportingEnabled == true && delegation.revokedAt == null,
            lastHeartbeatAt = runner?.lastHeartbeatAt,
            components = runner?.components ?: emptyMap(),
            probes = runner?.probes ?: emptyMap(),
        )
    }
}
