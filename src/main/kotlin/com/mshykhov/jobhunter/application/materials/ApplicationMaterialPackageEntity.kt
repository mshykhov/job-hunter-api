package com.mshykhov.jobhunter.application.materials

import com.mshykhov.jobhunter.application.job.JobEntity
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
import org.springframework.data.domain.Persistable
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "application_material_packages")
class ApplicationMaterialPackageEntity(
    @Id private val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) val user: UserEntity,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "job_id", nullable = false) val job: JobEntity,
    @Column(name = "selected_revision_id") var selectedRevisionId: UUID? = null,
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
