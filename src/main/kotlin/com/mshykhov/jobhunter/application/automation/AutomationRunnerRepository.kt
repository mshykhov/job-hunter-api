package com.mshykhov.jobhunter.application.automation

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface AutomationRunnerRepository : JpaRepository<AutomationRunnerEntity, UUID> {
    fun findByDelegationId(delegationId: UUID): AutomationRunnerEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select runner from AutomationRunnerEntity runner where runner.delegation.id = :delegationId")
    fun findForUpdateByDelegationId(@Param("delegationId") delegationId: UUID): AutomationRunnerEntity?
}
