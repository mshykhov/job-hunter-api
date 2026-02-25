package com.mshykhov.jobhunter.application.preference

import com.mshykhov.jobhunter.application.user.UserEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
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
@Table(name = "user_preferences")
@EntityListeners(AuditingEntityListener::class)
class UserPreferenceEntity(
    @Id
    private val id: UUID = UUID.randomUUID(),
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    val user: UserEntity,
    @Column(name = "raw_input", columnDefinition = "TEXT")
    var rawInput: String? = null,
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "categories", columnDefinition = "text[]")
    var categories: List<String> = emptyList(),
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "seniority_levels", columnDefinition = "text[]")
    var seniorityLevels: List<String> = emptyList(),
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "keywords", columnDefinition = "text[]")
    var keywords: List<String> = emptyList(),
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "excluded_keywords", columnDefinition = "text[]")
    var excludedKeywords: List<String> = emptyList(),
    @Column(name = "min_salary")
    var minSalary: Int? = null,
    @Column(name = "remote_only", nullable = false)
    var remoteOnly: Boolean = false,
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "enabled_sources", columnDefinition = "text[]")
    var enabledSources: List<String> = emptyList(),
    @Column(name = "notifications_enabled", nullable = false)
    var notificationsEnabled: Boolean = true,
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
