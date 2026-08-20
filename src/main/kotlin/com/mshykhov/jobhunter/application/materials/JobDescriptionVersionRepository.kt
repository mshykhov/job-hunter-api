package com.mshykhov.jobhunter.application.materials

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface JobDescriptionVersionRepository : JpaRepository<JobDescriptionVersionEntity, UUID> {
    fun findByUserIdAndJobIdAndContentSha256(userId: UUID, jobId: UUID, contentSha256: String): JobDescriptionVersionEntity?
}
