package com.mshykhov.jobhunter.persistence.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.UpdateTimestamp
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(name = "user_preferences")
class UserPreferenceEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "user_sub", unique = true, nullable = false)
    val userSub: String,
    @Column(name = "notifications_enabled", nullable = false)
    var notificationsEnabled: Boolean = true,
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "languages", columnDefinition = "text[]")
    var languages: List<String> = emptyList(),
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "excluded_keywords", columnDefinition = "text[]")
    var excludedKeywords: List<String> = emptyList(),
    @Column(name = "remote_only", nullable = false)
    var remoteOnly: Boolean = false,
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "enabled_sources", columnDefinition = "text[]")
    var enabledSources: List<String> = emptyList(),
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant? = null,
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null,
)
