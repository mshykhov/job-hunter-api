package com.mshykhov.jobhunter.application.userjob

import com.mshykhov.jobhunter.application.job.JobEntity
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
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.domain.Persistable
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "user_jobs",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "job_id"])],
)
@EntityListeners(AuditingEntityListener::class)
class UserJobEntity(
    @Id
    private val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    val job: JobEntity,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: UserJobStatus = UserJobStatus.NEW,
    @Column(name = "ai_relevance_score", nullable = false)
    val aiRelevanceScore: Int,
    @Column(name = "ai_reasoning", columnDefinition = "TEXT", nullable = false)
    val aiReasoning: String,
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
