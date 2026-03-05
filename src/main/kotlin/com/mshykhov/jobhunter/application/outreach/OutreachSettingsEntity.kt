package com.mshykhov.jobhunter.application.outreach

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
@Table(name = "outreach_settings")
@EntityListeners(AuditingEntityListener::class)
class OutreachSettingsEntity(
    @Id
    private val id: UUID = UUID.randomUUID(),
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    val user: UserEntity,
    @Column(name = "cover_letter_prompt", columnDefinition = "TEXT")
    var coverLetterPrompt: String? = null,
    @Column(name = "recruiter_message_prompt", columnDefinition = "TEXT")
    var recruiterMessagePrompt: String? = null,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_config", nullable = false, columnDefinition = "jsonb")
    var sourceConfig: Map<String, OutreachSourceConfig> = emptyMap(),
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
