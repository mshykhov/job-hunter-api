package com.mshykhov.jobhunter.application.automation.workflow

import jakarta.persistence.LockModeType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface AutomationWorkflowRunRepository : JpaRepository<AutomationWorkflowRunEntity, UUID> {
    fun findByDelegationIdAndIdempotencyKey(
        delegationId: UUID,
        idempotencyKey: UUID,
    ): AutomationWorkflowRunEntity?

    fun findAllByDelegationIdOrderByCreatedAtDesc(
        delegationId: UUID,
        pageable: Pageable,
    ): List<AutomationWorkflowRunEntity>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from AutomationWorkflowRunEntity r where r.id = :id")
    fun findForUpdate(id: UUID): AutomationWorkflowRunEntity?
}
