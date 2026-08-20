package com.mshykhov.jobhunter.application.materials

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
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.domain.Persistable
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "application_material_requests")
class ApplicationMaterialRequestEntity(
    @Id private val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "package_id", nullable = false)
    val materialPackage: ApplicationMaterialPackageEntity,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "job_description_version_id", nullable = false)
    val jobDescriptionVersion: JobDescriptionVersionEntity,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "profile_version_id", nullable = false)
    val profileVersion: CandidateProfileVersionEntity,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "fact_catalog_version_id", nullable = false)
    val factCatalogVersion: FactCatalogVersionEntity,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "writing_style_version_id", nullable = false)
    val writingStyleVersion: WritingStyleVersionEntity,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) var status: MaterialStatus,
    @Enumerated(EnumType.STRING) @Column(name = "request_mode", nullable = false, length = 32)
    val requestMode: MaterialRequestMode,
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "requested_kinds", nullable = false, columnDefinition = "jsonb")
    val requestedKinds: Set<MaterialKind>,
    @Enumerated(EnumType.STRING) @Column(name = "cover_letter_policy", nullable = false, length = 32)
    val coverLetterPolicy: CoverLetterPolicy,
    @Column(name = "encrypted_owner_edits", columnDefinition = "bytea") val encryptedOwnerEdits: ByteArray? = null,
    @Column(name = "generation_policy_version", nullable = false, length = 64) val generationPolicyVersion: String,
    @Column(name = "schema_version", nullable = false, length = 64) val schemaVersion: String,
    @Column(name = "renderer_version", nullable = false, length = 128) val rendererVersion: String,
    @Column(name = "model_route", nullable = false, length = 32) val modelRoute: String,
    @Column(name = "input_sha256", nullable = false, length = 64) val inputSha256: String,
    @Column(name = "lease_owner", length = 128) var leaseOwner: String? = null,
    @Column(name = "lease_token") var leaseToken: UUID? = null,
    @Column(name = "lease_expires_at") var leaseExpiresAt: Instant? = null,
    @Column(name = "attempt_count", nullable = false) var attemptCount: Int = 0,
    @Column(name = "idempotency_key", nullable = false, length = 128) val idempotencyKey: String,
    @Column(name = "parent_revision_id") val parentRevisionId: UUID? = null,
    @Column(name = "created_at", insertable = false, updatable = false) val createdAt: Instant? = null,
    @Column(name = "updated_at", insertable = false, updatable = false) val updatedAt: Instant? = null,
) : Persistable<UUID> {
    @Transient private var isNew = true
    override fun getId() = id
    override fun isNew() = isNew

    @PostPersist @PostLoad
    private fun markNotNew() {
        isNew = false
    }
}
