package com.mshykhov.jobhunter.application.automation

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
@Table(name = "automation_runner_transitions")
class AutomationRunnerTransitionEntity(
    @Id
    private val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "runner_id", nullable = false)
    val runner: AutomationRunnerEntity,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    val component: AutomationComponent,
    @Enumerated(EnumType.STRING)
    @Column(name = "from_state", nullable = false, length = 32)
    val fromState: AutomationState,
    @Enumerated(EnumType.STRING)
    @Column(name = "to_state", nullable = false, length = 32)
    val toState: AutomationState,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    val reason: AutomationReason,
    @Column(name = "occurred_at", nullable = false)
    val occurredAt: Instant,
    @Column(nullable = false)
    val generation: Long,
    @Column(nullable = false)
    val sequence: Long,
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
