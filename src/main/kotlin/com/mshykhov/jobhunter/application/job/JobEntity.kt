package com.mshykhov.jobhunter.application.job

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
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
@Table(name = "jobs")
@EntityListeners(AuditingEntityListener::class)
class JobEntity(
    @Id
    private val id: UUID = UUID.randomUUID(),
    @Column(nullable = false)
    var title: String,
    val company: String? = null,
    @Column(nullable = false, unique = true)
    val url: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    var description: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val source: JobSource,
    var salary: String? = null,
    var location: String? = null,
    @Column(nullable = false)
    var remote: Boolean = false,
    @Column(name = "published_at")
    var publishedAt: Instant? = null,
    @Column(name = "matched_at")
    var matchedAt: Instant? = null,
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    var createdAt: Instant? = null,
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
