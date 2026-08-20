package com.mshykhov.jobhunter.application.materials

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface WritingStyleVersionRepository : JpaRepository<WritingStyleVersionEntity, UUID> {
    fun findByUserIdAndContentSha256(userId: UUID, contentSha256: String): WritingStyleVersionEntity?
    fun findByUserIdAndActiveTrue(userId: UUID): WritingStyleVersionEntity?
}
