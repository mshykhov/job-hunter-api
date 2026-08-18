package com.mshykhov.jobhunter.application.automation

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
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.domain.Persistable
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "automation_runners")
@EntityListeners(AuditingEntityListener::class)
class AutomationRunnerEntity(
    @Id
    private val id: UUID = UUID.randomUUID(),
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delegation_id", nullable = false, unique = true)
    val delegation: AutomationDelegationEntity,
    @Column(name = "runner_key", nullable = false, unique = true, length = 64)
    val runnerKey: String,
    @Column(nullable = false)
    var generation: Long = 0,
    @Column(nullable = false)
    var sequence: Long = 0,
    @Column(name = "last_idempotency_key")
    var lastIdempotencyKey: UUID? = null,
    @Column(name = "launcher_version", length = 64)
    var launcherVersion: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "overall_state", nullable = false, length = 32)
    var overallState: AutomationState = AutomationState.UNAVAILABLE,
    @Enumerated(EnumType.STRING)
    @Column(name = "overall_reason", nullable = false, length = 64)
    var overallReason: AutomationReason = AutomationReason.INVALID_REPORT,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    var components: Map<AutomationComponent, AutomationComponentSnapshot> = emptyMap(),
    @Column(name = "last_heartbeat_at")
    var lastHeartbeatAt: Instant? = null,
    @Column(name = "last_preflight_success_at")
    var lastPreflightSuccessAt: Instant? = null,
    @Column(name = "last_codex_success_at")
    var lastCodexSuccessAt: Instant? = null,
    @Column(name = "codex_input_tokens", nullable = false)
    var codexInputTokens: Long = 0,
    @Column(name = "codex_output_tokens", nullable = false)
    var codexOutputTokens: Long = 0,
    @CreatedDate
    @Column(name = "created_at", insertable = false, updatable = false)
    val createdAt: Instant? = null,
    @LastModifiedDate
    @Column(name = "updated_at", insertable = false)
    var updatedAt: Instant? = null,
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
