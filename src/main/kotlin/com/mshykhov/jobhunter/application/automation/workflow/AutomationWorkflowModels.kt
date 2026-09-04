package com.mshykhov.jobhunter.application.automation.workflow

import java.time.Instant
import java.util.UUID

data class LeaseCommand(val attemptId: UUID, val leaseToken: UUID, val generation: Long)

data class CheckpointCommand(
    val attemptId: UUID,
    val leaseToken: UUID,
    val generation: Long,
    val idempotencyKey: UUID,
    val step: SyntheticWorkflowStep,
    val evidenceSha256: String,
)

data class FailureCommand(
    val attemptId: UUID,
    val leaseToken: UUID,
    val generation: Long,
    val retryable: Boolean,
    val code: String,
    val detail: String? = null,
)

data class WorkClaim(
    val runId: UUID,
    val workItemId: UUID,
    val attemptId: UUID,
    val leaseToken: UUID,
    val leaseExpiresAt: Instant,
    val generation: Long,
    val nextStepIndex: Int,
    val steps: List<SyntheticWorkflowStep> = SyntheticWorkflowStep.entries,
)

data class WorkProgress(
    val runId: UUID,
    val workItemId: UUID,
    val runStatus: AutomationWorkflowStatus,
    val workItemStatus: AutomationWorkItemStatus,
    val completedSteps: Int,
    val leaseExpiresAt: Instant?,
)

data class WorkflowRunSummary(
    val id: UUID,
    val runType: String,
    val status: AutomationWorkflowStatus,
    val completedSteps: Int,
    val attemptCount: Int,
    val failureCode: String?,
    val createdAt: Instant?,
    val updatedAt: Instant?,
    val completedAt: Instant?,
)

data class WorkflowAttemptView(
    val id: UUID,
    val attemptNumber: Int,
    val workerId: String,
    val runnerGeneration: Long,
    val startedAt: Instant,
    val lastHeartbeatAt: Instant,
    val finishedAt: Instant?,
    val outcome: AutomationAttemptOutcome,
    val failureCode: String?,
)

data class WorkflowCheckpointView(
    val id: UUID,
    val attemptId: UUID,
    val step: SyntheticWorkflowStep,
    val stepIndex: Int,
    val evidenceSha256: String,
    val createdAt: Instant?,
)

data class WorkflowEventView(val id: UUID, val eventType: String, val payload: Map<String, String>, val occurredAt: Instant)

data class WorkflowRunView(
    val id: UUID,
    val runType: String,
    val status: AutomationWorkflowStatus,
    val workItemId: UUID,
    val workItemStatus: AutomationWorkItemStatus,
    val completedSteps: Int,
    val attemptCount: Int,
    val failureCode: String?,
    val failureDetail: String?,
    val createdAt: Instant?,
    val updatedAt: Instant?,
    val completedAt: Instant?,
    val attempts: List<WorkflowAttemptView>,
    val checkpoints: List<WorkflowCheckpointView>,
    val events: List<WorkflowEventView>,
)
