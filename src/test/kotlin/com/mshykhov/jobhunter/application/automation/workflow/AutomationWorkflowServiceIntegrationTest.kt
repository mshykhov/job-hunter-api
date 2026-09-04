package com.mshykhov.jobhunter.application.automation.workflow

import com.mshykhov.jobhunter.application.automation.AutomationService
import com.mshykhov.jobhunter.application.common.AutomationLeaseLostException
import com.mshykhov.jobhunter.application.common.ConflictException
import com.mshykhov.jobhunter.application.common.StaleAutomationGenerationException
import com.mshykhov.jobhunter.support.AbstractIntegrationTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class AutomationWorkflowServiceIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var automationService: AutomationService

    @Autowired
    lateinit var workflowService: AutomationWorkflowService

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    private var generation: Long = 0

    @BeforeEach
    fun enableRunner() {
        jdbcTemplate.update("DELETE FROM automation_workflow_events")
        jdbcTemplate.update("DELETE FROM automation_workflow_checkpoints")
        jdbcTemplate.update("DELETE FROM automation_work_attempts")
        jdbcTemplate.update("DELETE FROM automation_work_items")
        jdbcTemplate.update("DELETE FROM automation_workflow_runs")
        automationService.enableDelegation()
        generation = automationService.startSession().generation
    }

    @Test
    fun `creates a run idempotently and completes every synthetic step once`() {
        val idempotencyKey = UUID.randomUUID()
        val created = workflowService.createRun(idempotencyKey)
        val replay = workflowService.createRun(idempotencyKey)

        assertEquals(created.id, replay.id)

        val claim = assertNotNull(workflowService.claim("integration-worker", generation))
        SyntheticWorkflowStep.entries.forEach { step ->
            val command =
                CheckpointCommand(
                    attemptId = claim.attemptId,
                    leaseToken = claim.leaseToken,
                    generation = generation,
                    idempotencyKey = UUID.randomUUID(),
                    step = step,
                    evidenceSha256 = "a".repeat(64),
                )
            val progress = workflowService.checkpoint(claim.workItemId, command)
            val replayed = workflowService.checkpoint(claim.workItemId, command)
            assertEquals(progress, replayed)
        }

        val completed =
            workflowService.complete(
                claim.workItemId,
                LeaseCommand(claim.attemptId, claim.leaseToken, generation),
            )
        val replayed =
            workflowService.complete(
                claim.workItemId,
                LeaseCommand(claim.attemptId, claim.leaseToken, generation),
            )

        assertEquals(AutomationWorkflowStatus.SUCCEEDED, completed.runStatus)
        assertEquals(3, completed.completedSteps)
        assertEquals(completed, replayed)
        assertEquals(3, workflowService.getRun(created.id).checkpoints.size)
    }

    @Test
    fun `new runner generation fences the old lease and resumes from its checkpoint`() {
        val run = workflowService.createRun(UUID.randomUUID())
        val firstClaim = assertNotNull(workflowService.claim("old-worker", generation))
        workflowService.checkpoint(
            firstClaim.workItemId,
            CheckpointCommand(
                firstClaim.attemptId,
                firstClaim.leaseToken,
                generation,
                UUID.randomUUID(),
                SyntheticWorkflowStep.PREPARE,
                "b".repeat(64),
            ),
        )

        val nextGeneration = automationService.startSession().generation

        assertFailsWith<StaleAutomationGenerationException> {
            workflowService.heartbeat(
                firstClaim.workItemId,
                LeaseCommand(firstClaim.attemptId, firstClaim.leaseToken, generation),
            )
        }
        val resumed = assertNotNull(workflowService.claim("new-worker", nextGeneration))
        assertEquals(run.id, resumed.runId)
        assertEquals(1, resumed.nextStepIndex)
        assertNotEquals(firstClaim.attemptId, resumed.attemptId)
        assertEquals(
            AutomationAttemptOutcome.STALE_GENERATION,
            workflowService.getRun(run.id).attempts.first().outcome,
        )
    }

    @Test
    fun `pause revokes a lease and resume creates a new attempt while stop is terminal`() {
        val run = workflowService.createRun(UUID.randomUUID())
        val claim = assertNotNull(workflowService.claim("worker", generation))

        assertEquals(AutomationWorkflowStatus.PAUSED, workflowService.pause(run.id).status)
        assertFailsWith<AutomationLeaseLostException> {
            workflowService.heartbeat(
                claim.workItemId,
                LeaseCommand(claim.attemptId, claim.leaseToken, generation),
            )
        }
        assertEquals(null, workflowService.claim("worker", generation))

        workflowService.resume(run.id)
        val resumed = assertNotNull(workflowService.claim("worker", generation))
        assertNotEquals(claim.attemptId, resumed.attemptId)
        assertEquals(AutomationWorkflowStatus.STOPPED, workflowService.stop(run.id).status)
        assertFailsWith<ConflictException> { workflowService.resume(run.id) }
    }

    @Test
    fun `expired leases retry at most three times and then fail durably`() {
        val run = workflowService.createRun(UUID.randomUUID())

        repeat(3) { attemptIndex ->
            val claim = assertNotNull(workflowService.claim("expiring-worker", generation))
            assertEquals(attemptIndex + 1, workflowService.getRun(run.id).attemptCount)
            jdbcTemplate.update(
                "UPDATE automation_work_items SET lease_expires_at = now() - interval '1 second' WHERE id = ?",
                claim.workItemId,
            )
        }

        assertEquals(null, workflowService.claim("expiring-worker", generation))
        val failed = workflowService.getRun(run.id)
        assertEquals(AutomationWorkflowStatus.FAILED, failed.status)
        assertEquals("LEASE_EXHAUSTED", failed.failureCode)
        assertEquals(
            listOf(
                AutomationAttemptOutcome.LEASE_EXPIRED,
                AutomationAttemptOutcome.LEASE_EXPIRED,
                AutomationAttemptOutcome.LEASE_EXPIRED,
            ),
            failed.attempts.map { it.outcome },
        )
    }
}
