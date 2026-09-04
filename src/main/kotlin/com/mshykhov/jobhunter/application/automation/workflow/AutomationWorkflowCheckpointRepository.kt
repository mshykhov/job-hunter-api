package com.mshykhov.jobhunter.application.automation.workflow

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AutomationWorkflowCheckpointRepository : JpaRepository<AutomationWorkflowCheckpointEntity, UUID> {
    fun findByWorkItemIdAndIdempotencyKey(
        workItemId: UUID,
        idempotencyKey: UUID,
    ): AutomationWorkflowCheckpointEntity?

    fun findByWorkItemIdAndStepIndex(
        workItemId: UUID,
        stepIndex: Int,
    ): AutomationWorkflowCheckpointEntity?

    fun findAllByWorkItemIdOrderByStepIndex(workItemId: UUID): List<AutomationWorkflowCheckpointEntity>
}
