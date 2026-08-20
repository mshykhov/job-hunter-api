package com.mshykhov.jobhunter.application.statistics

import com.mshykhov.jobhunter.application.job.JobGroupEntity
import com.mshykhov.jobhunter.application.user.UserEntity
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
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.domain.Persistable
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "user_job_group_decisions", uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "group_id"])])
@EntityListeners(AuditingEntityListener::class)
class UserJobGroupDecisionEntity(
    @Id private val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) val user: UserEntity,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "group_id", nullable = false) val group: JobGroupEntity,
    @Column(name = "vacancy_seen_at", nullable = false, updatable = false) val vacancySeenAt: Instant,
    @Column(name = "decided_at", nullable = false) var decidedAt: Instant,
    @Enumerated(EnumType.STRING) @Column(nullable = false) var outcome: DecisionOutcome,
    @Column(name = "cold_filter", length = 100) var coldFilter: String? = null,
    @Column(name = "ai_score") var aiScore: Int? = null,
    @Column(name = "inferred_remote") var inferredRemote: Boolean? = null,
    @JdbcTypeCode(SqlTypes.ARRAY) @Column(nullable = false, columnDefinition = "varchar[]") var sources: Array<String> = emptyArray(),
    @JdbcTypeCode(SqlTypes.ARRAY) @Column(nullable = false, columnDefinition = "varchar[]") var categories: Array<String> = emptyArray(),
    @CreatedDate @Column(name = "created_at", insertable = false, updatable = false) val createdAt: Instant? = null,
    @LastModifiedDate @Column(name = "updated_at", insertable = false) var updatedAt: Instant? = null,
) : Persistable<UUID> {
    @Transient private var isNew = true
    override fun getId(): UUID = id
    override fun isNew(): Boolean = isNew

    @PostPersist @PostLoad
    private fun markNotNew() {
        isNew = false
    }
    fun update(
        outcome: DecisionOutcome,
        coldFilter: String?,
        aiScore: Int?,
        inferredRemote: Boolean?,
        sources: Array<String>,
        categories: Array<String>,
        decidedAt: Instant,
    ) {
        this.outcome = outcome
        this.coldFilter = coldFilter?.take(100)
        this.aiScore = aiScore
        this.inferredRemote = inferredRemote
        this.sources = sources
        this.categories = categories
        this.decidedAt = decidedAt
    }
}
