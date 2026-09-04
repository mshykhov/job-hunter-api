package com.mshykhov.jobhunter.application.automation.workflow

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AutomationWorkAttemptRepository : JpaRepository<AutomationWorkAttemptEntity, UUID> {
    fun findByWorkItemIdAndOutcome(
        workItemId: UUID,
        outcome: AutomationAttemptOutcome,
    ): AutomationWorkAttemptEntity?

    fun findAllByWorkItemIdOrderByAttemptNumber(workItemId: UUID): List<AutomationWorkAttemptEntity>
}
