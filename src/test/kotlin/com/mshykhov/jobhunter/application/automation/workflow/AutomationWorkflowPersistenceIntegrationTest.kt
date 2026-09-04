package com.mshykhov.jobhunter.application.automation.workflow

import com.mshykhov.jobhunter.application.automation.AutomationService
import com.mshykhov.jobhunter.infrastructure.metrics.AutomationWorkflowMetricReader
import com.mshykhov.jobhunter.support.AbstractIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class AutomationWorkflowPersistenceIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var automationService: AutomationService

    @Autowired
    lateinit var workflowService: AutomationWorkflowService

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var metricReader: AutomationWorkflowMetricReader

    @Test
    fun `schema rejects an invalid checkpoint digest`() {
        automationService.enableDelegation()
        val generation = automationService.startSession().generation
        workflowService.createRun(UUID.randomUUID())
        val claim = assertNotNull(workflowService.claim("schema-worker", generation))

        kotlin.test.assertEquals(1, metricReader.read().active)

        assertFailsWith<DataIntegrityViolationException> {
            jdbcTemplate.update(
                """
                INSERT INTO automation_workflow_checkpoints
                    (work_item_id, attempt_id, step, step_index, idempotency_key, evidence_sha256)
                VALUES (?, ?, 'PREPARE', 0, ?, 'not-a-sha')
                """.trimIndent(),
                claim.workItemId,
                claim.attemptId,
                UUID.randomUUID(),
            )
        }
    }
}
