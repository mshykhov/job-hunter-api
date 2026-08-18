package com.mshykhov.jobhunter.application.automation

import com.mshykhov.jobhunter.application.user.UserEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
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
@Table(name = "automation_delegations")
@EntityListeners(AuditingEntityListener::class)
class AutomationDelegationEntity(
    @Id
    private val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity,
    @Column(name = "owner_issuer", nullable = false, length = 512)
    val ownerIssuer: String,
    @Column(name = "owner_subject", nullable = false, length = 512)
    val ownerSubject: String,
    @Column(name = "runner_issuer", nullable = false, length = 512)
    val runnerIssuer: String,
    @Column(name = "health_reporting_enabled", nullable = false)
    var healthReportingEnabled: Boolean = true,
    @CreatedDate
    @Column(name = "created_at", insertable = false, updatable = false)
    val createdAt: Instant? = null,
    @LastModifiedDate
    @Column(name = "updated_at", insertable = false)
    var updatedAt: Instant? = null,
    @Column(name = "revoked_at")
    var revokedAt: Instant? = null,
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
