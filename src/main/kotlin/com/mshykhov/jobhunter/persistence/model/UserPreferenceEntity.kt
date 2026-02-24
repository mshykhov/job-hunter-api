package com.mshykhov.jobhunter.persistence.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
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
@Table(name = "user_preferences")
@EntityListeners(AuditingEntityListener::class)
class UserPreferenceEntity(
    @Id
    private val id: UUID = UUID.randomUUID(),
    @Column(name = "user_sub", unique = true, nullable = false)
    val userSub: String,
    @Column(name = "notifications_enabled", nullable = false)
    var notificationsEnabled: Boolean = true,
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "categories", columnDefinition = "text[]")
    var categories: List<String> = emptyList(),
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "excluded_keywords", columnDefinition = "text[]")
    var excludedKeywords: List<String> = emptyList(),
    @Column(name = "remote_only", nullable = false)
    var remoteOnly: Boolean = false,
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "enabled_sources", columnDefinition = "text[]")
    var enabledSources: List<String> = emptyList(),
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
