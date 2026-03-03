package com.mshykhov.jobhunter.application.userjob

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface UserJobRepository :
    JpaRepository<UserJobEntity, UUID>,
    JpaSpecificationExecutor<UserJobEntity> {
    fun deleteByJobIdIn(jobIds: List<UUID>)

    @Query("SELECT uj.user.id FROM UserJobEntity uj WHERE uj.job.id = :jobId")
    fun findUserIdsByJobId(jobId: UUID): Set<UUID>

    @Query("SELECT uj FROM UserJobEntity uj JOIN FETCH uj.job WHERE uj.user.id = :userId AND uj.status = :status")
    fun findByUserIdAndStatus(
        userId: UUID,
        status: UserJobStatus,
    ): List<UserJobEntity>

    @Query("SELECT uj FROM UserJobEntity uj JOIN FETCH uj.job WHERE uj.user.id = :userId")
    fun findByUserId(userId: UUID): List<UserJobEntity>

    @Query("SELECT uj FROM UserJobEntity uj JOIN FETCH uj.job WHERE uj.user.id = :userId AND uj.job.id = :jobId")
    fun findByUserIdAndJobId(
        userId: UUID,
        jobId: UUID,
    ): UserJobEntity?
}
