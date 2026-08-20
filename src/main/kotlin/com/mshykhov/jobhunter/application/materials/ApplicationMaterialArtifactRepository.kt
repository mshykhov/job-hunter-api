package com.mshykhov.jobhunter.application.materials

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ApplicationMaterialArtifactRepository : JpaRepository<ApplicationMaterialArtifactEntity, UUID> {
    fun findByUserIdAndKindAndPlaintextSha256(
        userId: UUID,
        kind: MaterialKind,
        plaintextSha256: String,
    ): ApplicationMaterialArtifactEntity?

    fun findByIdAndUserId(id: UUID, userId: UUID): ApplicationMaterialArtifactEntity?
}
