package com.mshykhov.jobhunter.application.automation.workflow

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
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
@Table(name = "automation_work_items")
@EntityListeners(AuditingEntityListener::class)
class AutomationWorkItemEntity(
    @Id
    private val id: UUID = UUID.randomUUID(),
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false, unique = true)
    val run: AutomationWorkflowRunEntity,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var status: AutomationWorkItemStatus = AutomationWorkItemStatus.QUEUED,
    @Column(name = "lease_owner", length = 128)
    var leaseOwner: String? = null,
    @Column(name = "lease_token", unique = true)
    var leaseToken: UUID? = null,
    @Column(name = "lease_generation")
    var leaseGeneration: Long? = null,
    @Column(name = "lease_expires_at")
    var leaseExpiresAt: Instant? = null,
    @Column(name = "attempt_count", nullable = false)
    var attemptCount: Int = 0,
    @Column(name = "max_attempts", nullable = false)
    val maxAttempts: Int = 3,
    @Column(name = "next_step_index", nullable = false)
    var nextStepIndex: Int = 0,
    @Column(name = "failure_code", length = 64)
    var failureCode: String? = null,
    @Column(name = "failure_detail", length = 512)
    var failureDetail: String? = null,
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
