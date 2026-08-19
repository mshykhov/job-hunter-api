package com.mshykhov.jobhunter.application.automation

import com.mshykhov.jobhunter.application.user.UserRepository
import com.mshykhov.jobhunter.support.AbstractIntegrationTest
import com.mshykhov.jobhunter.support.TestFixtures
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AutomationPersistenceIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var automationFacade: AutomationFacade

    @Autowired
    lateinit var delegationRepository: AutomationDelegationRepository

    @Autowired
    lateinit var runnerRepository: AutomationRunnerRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Test
    fun `should round-trip runner snapshots through JSONB`() {
        val user = userRepository.save(TestFixtures.userEntity())
        val delegation = automationFacade.saveDelegation(TestFixtures.automationDelegationEntity(user = user))
        val checkedAt = Instant.parse("2026-08-18T08:00:00Z")
        val components =
            mapOf(
                AutomationComponent.LAUNCHER to
                    AutomationComponentSnapshot(
                        state = AutomationState.READY,
                        reason = AutomationReason.NONE,
                        checkedAt = checkedAt,
                        probeVersion = "0.1.0",
                    ),
            )
        val probes =
            mapOf(
                ProbeType.CODEX to
                    AutomationProbeSnapshot(
                        outcome = ProbeOutcome.SUCCESS,
                        reason = AutomationReason.NONE,
                        durationMillis = 1250,
                        consecutiveFailures = 0,
                        lastSuccessAt = checkedAt,
                    ),
            )

        automationFacade.saveRunner(
            TestFixtures.automationRunnerEntity(
                delegation = delegation,
                components = components,
                probes = probes,
            ),
        )

        val reloaded = automationFacade.findRunner(delegation.id)

        assertEquals(components, reloaded?.components)
        assertEquals(probes, reloaded?.probes)
    }

    @Test
    fun `should allow only one delegation for an owner identity`() {
        val user = userRepository.save(TestFixtures.userEntity())
        val ownerSubject = "owner-${UUID.randomUUID()}"
        delegationRepository.saveAndFlush(
            TestFixtures.automationDelegationEntity(user = user, ownerSubject = ownerSubject),
        )

        assertFailsWith<DataIntegrityViolationException> {
            delegationRepository.saveAndFlush(
                TestFixtures.automationDelegationEntity(user = user, ownerSubject = ownerSubject),
            )
        }
    }

    @Test
    fun `should allow only one runner for a delegation`() {
        val user = userRepository.save(TestFixtures.userEntity())
        val delegation = automationFacade.saveDelegation(TestFixtures.automationDelegationEntity(user = user))
        runnerRepository.saveAndFlush(TestFixtures.automationRunnerEntity(delegation = delegation))

        assertFailsWith<DataIntegrityViolationException> {
            runnerRepository.saveAndFlush(
                AutomationRunnerEntity(
                    delegation = delegation,
                    runnerKey = "replacement",
                ),
            )
        }
    }
}
