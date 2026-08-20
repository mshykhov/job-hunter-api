package com.mshykhov.jobhunter.application.materials

import com.mshykhov.jobhunter.application.user.UserEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
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
import org.springframework.data.domain.Persistable
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "application_material_artifacts")
class ApplicationMaterialArtifactEntity(
    @Id
    private val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    val kind: MaterialKind,
    @Column(name = "media_type", nullable = false, length = 128)
    val mediaType: String,
    @Column(name = "encrypted_content", nullable = false, columnDefinition = "bytea")
    val encryptedContent: ByteArray,
    @Column(name = "plaintext_sha256", nullable = false, length = 64)
    val plaintextSha256: String,
    @Column(name = "extraction_sha256", length = 64)
    val extractionSha256: String? = null,
    @Column(name = "byte_size", nullable = false)
    val byteSize: Long,
    @Column(name = "renderer_fingerprint", length = 128)
    val rendererFingerprint: String? = null,
    @Column(name = "retention_state", nullable = false, length = 32)
    val retentionState: String = "ACTIVE",
    @Column(name = "created_at", insertable = false, updatable = false)
    val createdAt: Instant? = null,
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
