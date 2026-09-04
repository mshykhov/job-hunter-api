package com.mshykhov.jobhunter.application.automation.workflow

import com.mshykhov.jobhunter.application.automation.AutomationDelegationEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
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
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.domain.Persistable
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "automation_workflow_runs")
@EntityListeners(AuditingEntityListener::class)
class AutomationWorkflowRunEntity(
    @Id
    private val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delegation_id", nullable = false)
    val delegation: AutomationDelegationEntity,
    @Column(name = "run_type", nullable = false, length = 64)
    val runType: String = "SYNTHETIC_RECOVERY",
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var status: AutomationWorkflowStatus = AutomationWorkflowStatus.QUEUED,
    @Column(name = "idempotency_key", nullable = false)
    val idempotencyKey: UUID,
    @CreatedDate
    @Column(name = "created_at", insertable = false, updatable = false)
    val createdAt: Instant? = null,
    @LastModifiedDate
    @Column(name = "updated_at", insertable = false)
    var updatedAt: Instant? = null,
    @Column(name = "completed_at")
    var completedAt: Instant? = null,
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
