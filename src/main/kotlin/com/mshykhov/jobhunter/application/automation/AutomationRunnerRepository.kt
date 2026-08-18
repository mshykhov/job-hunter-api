package com.mshykhov.jobhunter.application.automation

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AutomationRunnerRepository : JpaRepository<AutomationRunnerEntity, UUID> {
    fun findByDelegationId(delegationId: UUID): AutomationRunnerEntity?
}
