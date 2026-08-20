package com.mshykhov.jobhunter.application.materials

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ApplicationMaterialPackageRepository : JpaRepository<ApplicationMaterialPackageEntity, UUID> {
    fun findByUserIdAndJobId(userId: UUID, jobId: UUID): ApplicationMaterialPackageEntity?
    fun findByIdAndUserId(id: UUID, userId: UUID): ApplicationMaterialPackageEntity?
}
