package com.mshykhov.jobhunter.application.automation.workflow

import com.mshykhov.jobhunter.application.automation.AutomationDelegationEntity
import com.mshykhov.jobhunter.application.automation.AutomationFacade
import com.mshykhov.jobhunter.application.common.ConflictException
import com.mshykhov.jobhunter.application.common.NotFoundException
import com.mshykhov.jobhunter.application.common.ValidationException
import com.mshykhov.jobhunter.infrastructure.automation.AutomationProperties
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
class AutomationWorkflowService(
    private val runRepository: AutomationWorkflowRunRepository,
    private val workItemRepository: AutomationWorkItemRepository,
    private val attemptRepository: AutomationWorkAttemptRepository,
    private val checkpointRepository: AutomationWorkflowCheckpointRepository,
    private val eventRepository: AutomationWorkflowEventRepository,
    private val automationFacade: AutomationFacade,
    private val properties: AutomationProperties,
    private val clock: Clock,
) {
    @Transactional
    fun createRun(idempotencyKey: UUID): WorkflowRunView {
        val delegation = activeDelegationForUpdate()
        runRepository.findByDelegationIdAndIdempotencyKey(delegation.id, idempotencyKey)?.let {
            return toView(it)
        }
        val now = Instant.now(clock)
        val run =
            runRepository.save(
                AutomationWorkflowRunEntity(
                    delegation = delegation,
                    idempotencyKey = idempotencyKey,
                ),
            )
        val item = workItemRepository.save(AutomationWorkItemEntity(run = run))
        appendEvent(run, item, "RUN_CREATED", now)
        return toView(run, item)
    }

    @Transactional(readOnly = true)
    fun listRuns(limit: Int): List<WorkflowRunSummary> {
        val delegation = activeDelegation()
        val boundedLimit = limit.coerceIn(1, 100)
        return runRepository.findAllByDelegationIdOrderByCreatedAtDesc(delegation.id, PageRequest.of(0, boundedLimit)).map { run ->
            val item = requireNotNull(workItemRepository.findByRunId(run.id))
            WorkflowRunSummary(
                id = run.id,
                runType = run.runType,
                status = run.status,
                completedSteps = item.nextStepIndex,
                attemptCount = item.attemptCount,
                failureCode = item.failureCode,
                createdAt = run.createdAt,
                updatedAt = run.updatedAt,
                completedAt = run.completedAt,
            )
        }
    }

    @Transactional(readOnly = true)
    fun getRun(runId: UUID): WorkflowRunView {
        val run = runRepository.findById(runId).orElseThrow { NotFoundException("Automation workflow run not found") }
        requireOwned(run)
        return toView(run)
    }

    @Transactional
    fun claim(
        workerId: String,
        generation: Long,
    ): WorkClaim? {
        if (workerId.isBlank() || workerId.length > 128) throw ValidationException("Worker ID must contain 1 to 128 characters")
        val delegation = activeDelegation()
        val runner = automationFacade.findRunner(delegation.id) ?: throw NotFoundException("Runner session not found")
        if (generation != runner.generation) throw ConflictException("Stale runner generation")
        val now = Instant.now(clock)
        recoverExpired(now)
        val item = workItemRepository.claimNext() ?: return null
        val run = item.run
        val leaseToken = UUID.randomUUID()
        item.status = AutomationWorkItemStatus.LEASED
        item.leaseOwner = workerId
        item.leaseToken = leaseToken
        item.leaseGeneration = generation
        item.leaseExpiresAt = now.plus(LEASE_DURATION)
        item.attemptCount += 1
        item.failureCode = null
        item.failureDetail = null
        run.status = AutomationWorkflowStatus.RUNNING
        val attempt =
            attemptRepository.save(
                AutomationWorkAttemptEntity(
                    workItem = item,
                    attemptNumber = item.attemptCount,
                    leaseToken = leaseToken,
                    workerId = workerId,
                    runnerGeneration = generation,
                    startedAt = now,
                    lastHeartbeatAt = now,
                ),
            )
        workItemRepository.save(item)
        runRepository.save(run)
        appendEvent(run, item, "WORK_CLAIMED", now, mapOf("attempt" to item.attemptCount.toString()))
        return WorkClaim(
            runId = run.id,
            workItemId = item.id,
            attemptId = attempt.id,
            leaseToken = leaseToken,
            leaseExpiresAt = requireNotNull(item.leaseExpiresAt),
            generation = generation,
            nextStepIndex = item.nextStepIndex,
        )
    }

    @Transactional
    fun heartbeat(
        workItemId: UUID,
        command: LeaseCommand,
    ): WorkProgress {
        val now = Instant.now(clock)
        val lease = requireLease(workItemId, command, now)
        lease.item.leaseExpiresAt = now.plus(LEASE_DURATION)
        lease.attempt.lastHeartbeatAt = now
        workItemRepository.save(lease.item)
        attemptRepository.save(lease.attempt)
        return progress(lease.item)
    }

    @Transactional
    fun checkpoint(
        workItemId: UUID,
        command: CheckpointCommand,
    ): WorkProgress {
        if (!SHA256.matches(command.evidenceSha256)) throw ValidationException("Checkpoint evidence must be a lowercase SHA-256")
        checkpointRepository.findByWorkItemIdAndIdempotencyKey(workItemId, command.idempotencyKey)?.let { existing ->
            if (existing.step != command.step || existing.evidenceSha256 != command.evidenceSha256) {
                throw ConflictException("Checkpoint idempotency key was already used with different content")
            }
            return progress(existing.workItem)
        }
        val now = Instant.now(clock)
        val lease = requireLease(workItemId, command.toLeaseCommand(), now)
        val expectedStep = SyntheticWorkflowStep.entries.getOrNull(lease.item.nextStepIndex)
            ?: throw ConflictException("Every workflow step is already checkpointed")
        if (command.step != expectedStep) throw ConflictException("Unexpected workflow step")
        checkpointRepository.findByWorkItemIdAndStepIndex(workItemId, lease.item.nextStepIndex)?.let {
            throw ConflictException("Workflow step is already checkpointed")
        }
        checkpointRepository.save(
            AutomationWorkflowCheckpointEntity(
                workItem = lease.item,
                attempt = lease.attempt,
                step = command.step,
                stepIndex = lease.item.nextStepIndex,
                idempotencyKey = command.idempotencyKey,
                evidenceSha256 = command.evidenceSha256,
            ),
        )
        lease.item.nextStepIndex += 1
        lease.item.leaseExpiresAt = now.plus(LEASE_DURATION)
        lease.attempt.lastHeartbeatAt = now
        workItemRepository.save(lease.item)
        attemptRepository.save(lease.attempt)
        appendEvent(
            lease.item.run,
            lease.item,
            "STEP_CHECKPOINTED",
            now,
            mapOf("step" to command.step.name, "stepIndex" to (lease.item.nextStepIndex - 1).toString()),
        )
        return progress(lease.item)
    }

    @Transactional
    fun complete(
        workItemId: UUID,
        command: LeaseCommand,
    ): WorkProgress {
        val item = workItemRepository.findForUpdate(workItemId) ?: throw NotFoundException("Automation work item not found")
        if (item.status == AutomationWorkItemStatus.SUCCEEDED) return progress(item)
        val now = Instant.now(clock)
        val lease = requireLease(item, command, now)
        if (item.nextStepIndex != SyntheticWorkflowStep.entries.size) throw ConflictException("Workflow has incomplete steps")
        item.status = AutomationWorkItemStatus.SUCCEEDED
        item.completedAt = now
        clearLease(item)
        item.run.status = AutomationWorkflowStatus.SUCCEEDED
        item.run.completedAt = now
        lease.attempt.outcome = AutomationAttemptOutcome.SUCCEEDED
        lease.attempt.finishedAt = now
        workItemRepository.save(item)
        runRepository.save(item.run)
        attemptRepository.save(lease.attempt)
        appendEvent(item.run, item, "RUN_SUCCEEDED", now)
        return progress(item)
    }

    @Transactional
    fun fail(
        workItemId: UUID,
        command: FailureCommand,
    ): WorkProgress {
        validateFailure(command)
        val now = Instant.now(clock)
        val lease = requireLease(workItemId, command.toLeaseCommand(), now)
        lease.attempt.outcome = AutomationAttemptOutcome.FAILED
        lease.attempt.failureCode = command.code
        lease.attempt.finishedAt = now
        lease.item.failureCode = command.code
        lease.item.failureDetail = command.detail
        val retry = command.retryable && lease.item.attemptCount < lease.item.maxAttempts
        lease.item.status = if (retry) AutomationWorkItemStatus.QUEUED else AutomationWorkItemStatus.FAILED
        lease.item.run.status = if (retry) AutomationWorkflowStatus.QUEUED else AutomationWorkflowStatus.FAILED
        if (!retry) {
            lease.item.completedAt = now
            lease.item.run.completedAt = now
        }
        clearLease(lease.item)
        attemptRepository.save(lease.attempt)
        workItemRepository.save(lease.item)
        runRepository.save(lease.item.run)
        appendEvent(lease.item.run, lease.item, if (retry) "WORK_REQUEUED" else "RUN_FAILED", now, mapOf("code" to command.code))
        return progress(lease.item)
    }

    @Transactional
    fun pause(runId: UUID): WorkflowRunView = control(runId, AutomationWorkflowStatus.PAUSED, AutomationAttemptOutcome.PAUSED)

    @Transactional
    fun resume(runId: UUID): WorkflowRunView {
        val run = ownedRunForUpdate(runId)
        if (run.status != AutomationWorkflowStatus.PAUSED) throw ConflictException("Only a paused workflow can be resumed")
        val item = requireNotNull(workItemRepository.findForUpdateByRunId(run.id))
        run.status = AutomationWorkflowStatus.QUEUED
        item.status = AutomationWorkItemStatus.QUEUED
        runRepository.save(run)
        workItemRepository.save(item)
        appendEvent(run, item, "RUN_RESUMED", Instant.now(clock))
        return toView(run, item)
    }

    @Transactional
    fun stop(runId: UUID): WorkflowRunView = control(runId, AutomationWorkflowStatus.STOPPED, AutomationAttemptOutcome.STOPPED)

    @Transactional
    fun onRunnerGenerationStarted(
        delegationId: UUID,
        generation: Long,
    ) {
        val now = Instant.now(clock)
        workItemRepository.findStaleGenerationForUpdate(delegationId, generation).forEach { item ->
            closeAttempt(item, AutomationAttemptOutcome.STALE_GENERATION, now)
            clearLease(item)
            item.status = AutomationWorkItemStatus.QUEUED
            item.run.status = AutomationWorkflowStatus.QUEUED
            workItemRepository.save(item)
            runRepository.save(item.run)
            appendEvent(item.run, item, "LEASE_FENCED", now, mapOf("generation" to generation.toString()))
        }
    }

    private fun control(
        runId: UUID,
        target: AutomationWorkflowStatus,
        outcome: AutomationAttemptOutcome,
    ): WorkflowRunView {
        val run = ownedRunForUpdate(runId)
        if (run.status in TERMINAL_STATUSES) throw ConflictException("Terminal workflow cannot be changed")
        if (target == AutomationWorkflowStatus.PAUSED && run.status == AutomationWorkflowStatus.PAUSED) return toView(run)
        val now = Instant.now(clock)
        val item = requireNotNull(workItemRepository.findForUpdateByRunId(run.id))
        if (item.status == AutomationWorkItemStatus.LEASED) closeAttempt(item, outcome, now)
        clearLease(item)
        run.status = target
        item.status = if (target == AutomationWorkflowStatus.STOPPED) AutomationWorkItemStatus.CANCELLED else AutomationWorkItemStatus.QUEUED
        if (target == AutomationWorkflowStatus.STOPPED) {
            run.completedAt = now
            item.completedAt = now
        }
        runRepository.save(run)
        workItemRepository.save(item)
        appendEvent(run, item, if (target == AutomationWorkflowStatus.PAUSED) "RUN_PAUSED" else "RUN_STOPPED", now)
        return toView(run, item)
    }

    private fun recoverExpired(now: Instant) {
        workItemRepository.findExpiredForUpdate(now).forEach { item ->
            closeAttempt(item, AutomationAttemptOutcome.LEASE_EXPIRED, now)
            val retry = item.attemptCount < item.maxAttempts
            clearLease(item)
            item.status = if (retry) AutomationWorkItemStatus.QUEUED else AutomationWorkItemStatus.FAILED
            item.run.status = if (retry) AutomationWorkflowStatus.QUEUED else AutomationWorkflowStatus.FAILED
            if (!retry) {
                item.failureCode = "LEASE_EXHAUSTED"
                item.completedAt = now
                item.run.completedAt = now
            }
            workItemRepository.save(item)
            runRepository.save(item.run)
            appendEvent(item.run, item, if (retry) "LEASE_EXPIRED" else "RUN_FAILED", now)
        }
    }

    private fun requireLease(
        workItemId: UUID,
        command: LeaseCommand,
        now: Instant,
    ): ActiveLease {
        val item = workItemRepository.findForUpdate(workItemId) ?: throw NotFoundException("Automation work item not found")
        return requireLease(item, command, now)
    }

    private fun requireLease(
        item: AutomationWorkItemEntity,
        command: LeaseCommand,
        now: Instant,
    ): ActiveLease {
        val delegation = activeDelegation()
        if (item.run.delegation.id != delegation.id) throw ConflictException("Automation delegation is not active")
        val runner = automationFacade.findRunner(delegation.id) ?: throw NotFoundException("Runner session not found")
        if (command.generation != runner.generation || item.leaseGeneration != command.generation) {
            throw ConflictException("Stale runner generation")
        }
        if (item.status != AutomationWorkItemStatus.LEASED || item.leaseToken != command.leaseToken || item.leaseExpiresAt?.isAfter(now) != true) {
            throw ConflictException("Automation work lease is stale or invalid")
        }
        val attempt = attemptRepository.findByWorkItemIdAndOutcome(item.id, AutomationAttemptOutcome.ACTIVE)
            ?: throw ConflictException("Automation work attempt is not active")
        if (attempt.id != command.attemptId || attempt.leaseToken != command.leaseToken) {
            throw ConflictException("Automation work attempt does not own the lease")
        }
        return ActiveLease(item, attempt)
    }

    private fun closeAttempt(
        item: AutomationWorkItemEntity,
        outcome: AutomationAttemptOutcome,
        now: Instant,
    ) {
        attemptRepository.findByWorkItemIdAndOutcome(item.id, AutomationAttemptOutcome.ACTIVE)?.let {
            it.outcome = outcome
            it.finishedAt = now
            attemptRepository.save(it)
        }
    }

    private fun clearLease(item: AutomationWorkItemEntity) {
        item.leaseOwner = null
        item.leaseToken = null
        item.leaseGeneration = null
        item.leaseExpiresAt = null
    }

    private fun activeDelegation(): AutomationDelegationEntity =
        automationFacade.findActiveDelegation(properties.ownerIssuer, properties.ownerSubject)
            ?.takeIf { it.healthReportingEnabled }
            ?: throw NotFoundException("Active automation delegation not found")

    private fun activeDelegationForUpdate(): AutomationDelegationEntity =
        automationFacade.findActiveDelegationForUpdate(properties.ownerIssuer, properties.ownerSubject)
            ?.takeIf { it.healthReportingEnabled }
            ?: throw NotFoundException("Active automation delegation not found")

    private fun ownedRunForUpdate(runId: UUID): AutomationWorkflowRunEntity =
        (runRepository.findForUpdate(runId) ?: throw NotFoundException("Automation workflow run not found")).also(::requireOwned)

    private fun requireOwned(run: AutomationWorkflowRunEntity) {
        if (run.delegation.id != activeDelegation().id) throw NotFoundException("Automation workflow run not found")
    }

    private fun appendEvent(
        run: AutomationWorkflowRunEntity,
        item: AutomationWorkItemEntity?,
        type: String,
        now: Instant,
        payload: Map<String, String> = emptyMap(),
    ) {
        eventRepository.save(
            AutomationWorkflowEventEntity(
                run = run,
                workItem = item,
                eventType = type,
                payload = payload,
                occurredAt = now,
            ),
        )
    }

    private fun progress(item: AutomationWorkItemEntity) =
        WorkProgress(
            runId = item.run.id,
            workItemId = item.id,
            runStatus = item.run.status,
            workItemStatus = item.status,
            completedSteps = item.nextStepIndex,
            leaseExpiresAt = item.leaseExpiresAt,
        )

    private fun toView(run: AutomationWorkflowRunEntity): WorkflowRunView =
        toView(run, workItemRepository.findByRunId(run.id) ?: throw NotFoundException("Automation work item not found"))

    private fun toView(
        run: AutomationWorkflowRunEntity,
        item: AutomationWorkItemEntity,
    ): WorkflowRunView =
        WorkflowRunView(
            id = run.id,
            runType = run.runType,
            status = run.status,
            workItemId = item.id,
            workItemStatus = item.status,
            completedSteps = item.nextStepIndex,
            attemptCount = item.attemptCount,
            failureCode = item.failureCode,
            failureDetail = item.failureDetail,
            createdAt = run.createdAt,
            updatedAt = run.updatedAt,
            completedAt = run.completedAt,
            attempts =
            attemptRepository.findAllByWorkItemIdOrderByAttemptNumber(item.id).map {
                WorkflowAttemptView(
                    id = it.id,
                    attemptNumber = it.attemptNumber,
                    workerId = it.workerId,
                    runnerGeneration = it.runnerGeneration,
                    startedAt = it.startedAt,
                    lastHeartbeatAt = it.lastHeartbeatAt,
                    finishedAt = it.finishedAt,
                    outcome = it.outcome,
                    failureCode = it.failureCode,
                )
            },
            checkpoints =
            checkpointRepository.findAllByWorkItemIdOrderByStepIndex(item.id).map {
                WorkflowCheckpointView(it.id, it.attempt.id, it.step, it.stepIndex, it.evidenceSha256, it.createdAt)
            },
            events =
            eventRepository.findAllByRunIdOrderByOccurredAtAsc(run.id).map {
                WorkflowEventView(it.id, it.eventType, it.payload, it.occurredAt)
            },
        )

    private fun validateFailure(command: FailureCommand) {
        if (!FAILURE_CODE.matches(command.code)) throw ValidationException("Failure code must be an uppercase machine code")
        if ((command.detail?.length ?: 0) > 512) throw ValidationException("Failure detail must not exceed 512 characters")
    }

    private fun CheckpointCommand.toLeaseCommand() = LeaseCommand(attemptId, leaseToken, generation)

    private fun FailureCommand.toLeaseCommand() = LeaseCommand(attemptId, leaseToken, generation)

    private data class ActiveLease(val item: AutomationWorkItemEntity, val attempt: AutomationWorkAttemptEntity)

    private companion object {
        val LEASE_DURATION: Duration = Duration.ofSeconds(60)
        val TERMINAL_STATUSES = setOf(AutomationWorkflowStatus.STOPPED, AutomationWorkflowStatus.SUCCEEDED, AutomationWorkflowStatus.FAILED)
        val SHA256 = Regex("^[0-9a-f]{64}$")
        val FAILURE_CODE = Regex("^[A-Z][A-Z0-9_]{0,63}$")
    }
}
