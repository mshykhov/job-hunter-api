package com.mshykhov.jobhunter.persistence.repository

import com.mshykhov.jobhunter.persistence.model.UserJobEntity
import com.mshykhov.jobhunter.persistence.model.UserJobStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserJobRepository : JpaRepository<UserJobEntity, UUID> {
    fun findByUserIdAndStatus(
        userId: UUID,
        status: UserJobStatus,
    ): List<UserJobEntity>

    fun findByUserId(userId: UUID): List<UserJobEntity>

    fun findByUserIdAndJobId(
        userId: UUID,
        jobId: UUID,
    ): UserJobEntity?

    fun existsByUserIdAndJobId(
        userId: UUID,
        jobId: UUID,
    ): Boolean
}
