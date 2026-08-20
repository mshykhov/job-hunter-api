package com.mshykhov.jobhunter.application.materials

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "application_material_revision_artifacts")
@IdClass(ApplicationMaterialRevisionArtifactId::class)
class ApplicationMaterialRevisionArtifactEntity(
    @Id @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "revision_id", nullable = false)
    val revision: ApplicationMaterialRevisionEntity,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "artifact_id", nullable = false)
    val artifact: ApplicationMaterialArtifactEntity,
    @Id @Enumerated(EnumType.STRING) val kind: MaterialKind,
)
