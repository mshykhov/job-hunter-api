package com.mshykhov.jobhunter.application.automation.workflow

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AutomationWorkflowEventRepository : JpaRepository<AutomationWorkflowEventEntity, UUID> {
    fun findAllByRunIdOrderByOccurredAtAsc(runId: UUID): List<AutomationWorkflowEventEntity>
}
