package com.mshykhov.jobhunter.application.materials

import com.mshykhov.jobhunter.application.user.UserEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PostLoad
import jakarta.persistence.PostPersist
import jakarta.persistence.Table
import jakarta.persistence.Transient
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.domain.Persistable
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "candidate_profile_versions")
class CandidateProfileVersionEntity(
    @Id private val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) val user: UserEntity,
    @Column(name = "schema_version", nullable = false, length = 64) val schemaVersion: String,
    @Column(name = "profile_version", nullable = false, length = 64) val profileVersion: String,
    @Column(name = "content_sha256", nullable = false, length = 64) val contentSha256: String,
    @Column(name = "encrypted_content", nullable = false, columnDefinition = "bytea") val encryptedContent: ByteArray,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "fact_catalog_version_id", nullable = false)
    val factCatalogVersion: FactCatalogVersionEntity,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "base_docx_artifact_id", nullable = false)
    val baseDocxArtifact: ApplicationMaterialArtifactEntity,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "base_pdf_artifact_id", nullable = false)
    val basePdfArtifact: ApplicationMaterialArtifactEntity,
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "validation_metadata", nullable = false, columnDefinition = "jsonb")
    val validationMetadata: Map<String, Any?>,
    @Column(name = "source_commit", nullable = false, length = 64) val sourceCommit: String,
    @Column(nullable = false) var active: Boolean = false,
    @Column(name = "created_at", insertable = false, updatable = false) val createdAt: Instant? = null,
) : Persistable<UUID> {
    @Transient private var isNew = true
    override fun getId() = id
    override fun isNew() = isNew

    @PostPersist @PostLoad
    private fun markNotNew() {
        isNew = false
    }
}
