package com.mshykhov.jobhunter.application.materials

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface FactCatalogVersionRepository : JpaRepository<FactCatalogVersionEntity, UUID> {
    fun findByUserIdAndContentSha256(userId: UUID, contentSha256: String): FactCatalogVersionEntity?
}
