package com.mshykhov.jobhunter.application.materials

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CandidateProfileVersionRepository : JpaRepository<CandidateProfileVersionEntity, UUID> {
    fun findByUserIdAndProfileVersion(userId: UUID, profileVersion: String): CandidateProfileVersionEntity?
    fun findByUserIdAndActiveTrue(userId: UUID): CandidateProfileVersionEntity?
    fun findAllByUserIdOrderByCreatedAtDesc(userId: UUID): List<CandidateProfileVersionEntity>
}
