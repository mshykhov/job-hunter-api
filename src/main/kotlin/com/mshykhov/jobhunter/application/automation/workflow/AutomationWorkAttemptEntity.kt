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
@Table(name = "automation_work_attempts")
class AutomationWorkAttemptEntity(
    @Id
    private val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_item_id", nullable = false)
    val workItem: AutomationWorkItemEntity,
    @Column(name = "attempt_number", nullable = false)
    val attemptNumber: Int,
    @Column(name = "lease_token", nullable = false, unique = true)
    val leaseToken: UUID,
    @Column(name = "worker_id", nullable = false, length = 128)
    val workerId: String,
    @Column(name = "runner_generation", nullable = false)
    val runnerGeneration: Long,
    @Column(name = "started_at", nullable = false)
    val startedAt: Instant,
    @Column(name = "last_heartbeat_at", nullable = false)
    var lastHeartbeatAt: Instant,
    @Column(name = "finished_at")
    var finishedAt: Instant? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var outcome: AutomationAttemptOutcome = AutomationAttemptOutcome.ACTIVE,
    @Column(name = "failure_code", length = 64)
    var failureCode: String? = null,
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
