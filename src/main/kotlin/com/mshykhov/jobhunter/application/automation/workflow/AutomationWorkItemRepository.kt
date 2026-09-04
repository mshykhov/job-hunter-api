package com.mshykhov.jobhunter.application.automation.workflow

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.UUID

interface AutomationWorkItemRepository : JpaRepository<AutomationWorkItemEntity, UUID> {
    fun findByRunId(runId: UUID): AutomationWorkItemEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from AutomationWorkItemEntity w where w.run.id = :runId")
    fun findForUpdateByRunId(runId: UUID): AutomationWorkItemEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from AutomationWorkItemEntity w where w.id = :id")
    fun findForUpdate(id: UUID): AutomationWorkItemEntity?

    @Query(
        value =
        """
            SELECT w.* FROM automation_work_items w
            JOIN automation_workflow_runs r ON r.id = w.run_id
            WHERE w.status = 'QUEUED'
              AND r.status IN ('QUEUED', 'RUNNING')
              AND w.attempt_count < w.max_attempts
            ORDER BY w.created_at, w.id
            FOR UPDATE OF r, w SKIP LOCKED
            LIMIT 1
            """,
        nativeQuery = true,
    )
    fun claimNext(): AutomationWorkItemEntity?

    @Query(
        value =
        """
            SELECT w.* FROM automation_work_items w
            JOIN automation_workflow_runs r ON r.id = w.run_id
            WHERE w.status = 'LEASED' AND w.lease_expires_at <= :now
            ORDER BY w.lease_expires_at
            FOR UPDATE OF r, w SKIP LOCKED
            """,
        nativeQuery = true,
    )
    fun findExpiredForUpdate(now: Instant): List<AutomationWorkItemEntity>

    @Query(
        value =
        """
            SELECT w.* FROM automation_work_items w
            JOIN automation_workflow_runs r ON r.id = w.run_id
            WHERE w.status = 'LEASED'
              AND w.lease_generation < :generation
              AND r.delegation_id = :delegationId
            ORDER BY w.created_at
            FOR UPDATE OF r, w
            """,
        nativeQuery = true,
    )
    fun findStaleGenerationForUpdate(
        delegationId: UUID,
        generation: Long,
    ): List<AutomationWorkItemEntity>
}
