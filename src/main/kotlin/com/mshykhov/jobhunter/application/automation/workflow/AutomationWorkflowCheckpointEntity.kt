package com.mshykhov.jobhunter.application.automation.workflow

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PostLoad
import jakarta.persistence.PostPersist
import jakarta.persistence.Table
import jakarta.persistence.Transient
import org.springframework.data.domain.Persistable
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "automation_workflow_checkpoints")
class AutomationWorkflowCheckpointEntity(
    @Id
    private val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_item_id", nullable = false)
    val workItem: AutomationWorkItemEntity,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    val attempt: AutomationWorkAttemptEntity,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    val step: SyntheticWorkflowStep,
    @Column(name = "step_index", nullable = false)
    val stepIndex: Int,
    @Column(name = "idempotency_key", nullable = false)
    val idempotencyKey: UUID,
    @Column(name = "evidence_sha256", nullable = false, length = 64)
    val evidenceSha256: String,
    @Column(name = "created_at", insertable = false, updatable = false)
    val createdAt: Instant? = null,
) : Persistable<UUID> {
    @Transient
    private var isNew: Boolean = true

    override fun getId(): UUID = id

    override fun isNew(): Boolean = isNew

    @PostPersist
    @PostLoad
    private fun markNotNew() {
        isNew = false
    }
}
