package com.mshykhov.jobhunter.api.rest.automation.dto.workflow

import com.mshykhov.jobhunter.application.automation.workflow.AutomationAttemptOutcome
import com.mshykhov.jobhunter.application.automation.workflow.AutomationWorkItemStatus
import com.mshykhov.jobhunter.application.automation.workflow.AutomationWorkflowStatus
import com.mshykhov.jobhunter.application.automation.workflow.CheckpointCommand
import com.mshykhov.jobhunter.application.automation.workflow.FailureCommand
import com.mshykhov.jobhunter.application.automation.workflow.LeaseCommand
import com.mshykhov.jobhunter.application.automation.workflow.SyntheticWorkflowStep
import com.mshykhov.jobhunter.application.automation.workflow.WorkClaim
import com.mshykhov.jobhunter.application.automation.workflow.WorkProgress
import com.mshykhov.jobhunter.application.automation.workflow.WorkflowAttemptView
import com.mshykhov.jobhunter.application.automation.workflow.WorkflowCheckpointView
import com.mshykhov.jobhunter.application.automation.workflow.WorkflowEventView
import com.mshykhov.jobhunter.application.automation.workflow.WorkflowRunSummary
import com.mshykhov.jobhunter.application.automation.workflow.WorkflowRunView
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CreateAutomationWorkflowRunRequest(val idempotencyKey: UUID)

data class AutomationWorkClaimRequest(@field:NotBlank @field:Size(max = 128) val workerId: String, @field:Min(1) val generation: Long)

data class AutomationLeaseRequest(val attemptId: UUID, val leaseToken: UUID, @field:Min(1) val generation: Long) {
    fun toCommand() = LeaseCommand(attemptId, leaseToken, generation)
}

data class AutomationCheckpointRequest(
    val attemptId: UUID,
    val leaseToken: UUID,
    @field:Min(1) val generation: Long,
    val idempotencyKey: UUID,
    val step: SyntheticWorkflowStep,
    @field:Pattern(regexp = "^[0-9a-f]{64}$") val evidenceSha256: String,
) {
    fun toCommand() = CheckpointCommand(attemptId, leaseToken, generation, idempotencyKey, step, evidenceSha256)
}

data class AutomationFailureRequest(
    val attemptId: UUID,
    val leaseToken: UUID,
    @field:Min(1) val generation: Long,
    val retryable: Boolean,
    @field:Pattern(regexp = "^[A-Z][A-Z0-9_]{0,63}$") val code: String,
    @field:Size(max = 512) val detail: String? = null,
) {
    fun toCommand() = FailureCommand(attemptId, leaseToken, generation, retryable, code, detail)
}

data class AutomationWorkClaimResponse(
    val runId: UUID,
    val workItemId: UUID,
    val attemptId: UUID,
    val leaseToken: UUID,
    val leaseExpiresAt: Instant,
    val generation: Long,
    val nextStepIndex: Int,
    val steps: List<SyntheticWorkflowStep>,
) {
    companion object {
        fun from(claim: WorkClaim) =
            AutomationWorkClaimResponse(
                claim.runId,
                claim.workItemId,
                claim.attemptId,
                claim.leaseToken,
                claim.leaseExpiresAt,
                claim.generation,
                claim.nextStepIndex,
                claim.steps,
            )
    }
}

data class AutomationWorkProgressResponse(
    val runId: UUID,
    val workItemId: UUID,
    val runStatus: AutomationWorkflowStatus,
    val workItemStatus: AutomationWorkItemStatus,
    val completedSteps: Int,
    val leaseExpiresAt: Instant?,
) {
    companion object {
        fun from(progress: WorkProgress) =
            AutomationWorkProgressResponse(
                progress.runId,
                progress.workItemId,
                progress.runStatus,
                progress.workItemStatus,
                progress.completedSteps,
                progress.leaseExpiresAt,
            )
    }
}

data class AutomationWorkflowRunSummaryResponse(
    val id: UUID,
    val runType: String,
    val status: AutomationWorkflowStatus,
    val completedSteps: Int,
    val attemptCount: Int,
    val failureCode: String?,
    val createdAt: Instant?,
    val updatedAt: Instant?,
    val completedAt: Instant?,
) {
    companion object {
        fun from(run: WorkflowRunSummary) =
            AutomationWorkflowRunSummaryResponse(
                run.id,
                run.runType,
                run.status,
                run.completedSteps,
                run.attemptCount,
                run.failureCode,
                run.createdAt,
                run.updatedAt,
                run.completedAt,
            )
    }
}

data class AutomationWorkflowRunResponse(
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
    val attempts: List<AutomationWorkflowAttemptResponse>,
    val checkpoints: List<AutomationWorkflowCheckpointResponse>,
    val events: List<AutomationWorkflowEventResponse>,
) {
    companion object {
        fun from(run: WorkflowRunView) =
            AutomationWorkflowRunResponse(
                run.id,
                run.runType,
                run.status,
                run.workItemId,
                run.workItemStatus,
                run.completedSteps,
                run.attemptCount,
                run.failureCode,
                run.failureDetail,
                run.createdAt,
                run.updatedAt,
                run.completedAt,
                run.attempts.map(AutomationWorkflowAttemptResponse::from),
                run.checkpoints.map(AutomationWorkflowCheckpointResponse::from),
                run.events.map(AutomationWorkflowEventResponse::from),
            )
    }
}

data class AutomationWorkflowAttemptResponse(
    val id: UUID,
    val attemptNumber: Int,
    val workerId: String,
    val runnerGeneration: Long,
    val startedAt: Instant,
    val lastHeartbeatAt: Instant,
    val finishedAt: Instant?,
    val outcome: AutomationAttemptOutcome,
    val failureCode: String?,
) {
    companion object {
        fun from(attempt: WorkflowAttemptView) =
            AutomationWorkflowAttemptResponse(
                attempt.id,
                attempt.attemptNumber,
                attempt.workerId,
                attempt.runnerGeneration,
                attempt.startedAt,
                attempt.lastHeartbeatAt,
                attempt.finishedAt,
                attempt.outcome,
                attempt.failureCode,
            )
    }
}

data class AutomationWorkflowCheckpointResponse(
    val id: UUID,
    val attemptId: UUID,
    val step: SyntheticWorkflowStep,
    val stepIndex: Int,
    val evidenceSha256: String,
    val createdAt: Instant?,
) {
    companion object {
        fun from(checkpoint: WorkflowCheckpointView) =
            AutomationWorkflowCheckpointResponse(
                checkpoint.id,
                checkpoint.attemptId,
                checkpoint.step,
                checkpoint.stepIndex,
                checkpoint.evidenceSha256,
                checkpoint.createdAt,
            )
    }
}

data class AutomationWorkflowEventResponse(val id: UUID, val eventType: String, val payload: Map<String, String>, val occurredAt: Instant) {
    companion object {
        fun from(event: WorkflowEventView) = AutomationWorkflowEventResponse(event.id, event.eventType, event.payload, event.occurredAt)
    }
}
