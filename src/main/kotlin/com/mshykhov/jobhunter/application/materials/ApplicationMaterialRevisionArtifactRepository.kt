package com.mshykhov.jobhunter.application.materials

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ApplicationMaterialRevisionArtifactRepository : JpaRepository<ApplicationMaterialRevisionArtifactEntity, ApplicationMaterialRevisionArtifactId> {
    @EntityGraph(attributePaths = ["artifact"])
    fun findAllByRevisionId(revisionId: UUID): List<ApplicationMaterialRevisionArtifactEntity>

    @EntityGraph(attributePaths = ["artifact", "revision", "revision.materialPackage", "revision.materialPackage.user"])
    fun findByRevisionIdAndKind(revisionId: UUID, kind: MaterialKind): ApplicationMaterialRevisionArtifactEntity?
}
