package com.mshykhov.jobhunter.application.materials

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface ApplicationMaterialRevisionRepository : JpaRepository<ApplicationMaterialRevisionEntity, UUID> {
    fun findByIdAndMaterialPackageUserId(id: UUID, userId: UUID): ApplicationMaterialRevisionEntity?
    fun findAllByMaterialPackageIdOrderByRevisionNumberDesc(packageId: UUID): List<ApplicationMaterialRevisionEntity>
    fun findByRequestId(requestId: UUID): ApplicationMaterialRevisionEntity?

    @Query("select coalesce(max(r.revisionNumber), 0) from ApplicationMaterialRevisionEntity r where r.materialPackage.id = :packageId")
    fun findMaxRevisionNumber(packageId: UUID): Int
}
