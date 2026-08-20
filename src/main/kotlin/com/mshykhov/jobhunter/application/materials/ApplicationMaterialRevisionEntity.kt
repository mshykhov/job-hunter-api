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
@Table(name = "application_material_revisions")
class ApplicationMaterialRevisionEntity(
    @Id private val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "package_id", nullable = false)
    val materialPackage: ApplicationMaterialPackageEntity,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "request_id", nullable = false)
    val request: ApplicationMaterialRequestEntity,
    @Column(name = "revision_number", nullable = false) val revisionNumber: Int,
    @Column(name = "parent_revision_id") val parentRevisionId: UUID? = null,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) val origin: MaterialOrigin,
    @Column(name = "input_sha256", nullable = false, length = 64) val inputSha256: String,
    @Column(name = "generator_model", length = 64) val generatorModel: String?,
    @Column(name = "renderer_version", nullable = false, length = 128) val rendererVersion: String,
    @Column(name = "eligibility_state", nullable = false, length = 32) val eligibilityState: String,
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb") val manifest: Map<String, Any?>,
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
