package com.mshykhov.jobhunter.application.automation.workflow

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PostLoad
import jakarta.persistence.PostPersist
import jakarta.persistence.Table
import jakarta.persistence.Transient
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.domain.Persistable
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "automation_workflow_events")
class AutomationWorkflowEventEntity(
    @Id
    private val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    val run: AutomationWorkflowRunEntity,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_item_id")
    val workItem: AutomationWorkItemEntity? = null,
    @Column(name = "event_type", nullable = false, length = 64)
    val eventType: String,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    val payload: Map<String, String> = emptyMap(),
    @Column(name = "occurred_at", nullable = false)
    val occurredAt: Instant,
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
