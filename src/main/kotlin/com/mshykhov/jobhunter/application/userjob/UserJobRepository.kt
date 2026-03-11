package com.mshykhov.jobhunter.application.userjob

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface UserJobRepository : JpaRepository<UserJobEntity, UUID> {
    fun deleteByJobIdIn(jobIds: List<UUID>)

    @Query("SELECT uj FROM UserJobEntity uj JOIN FETCH uj.job WHERE uj.user.id = :userId AND uj.job.id = :jobId")
    fun findByUserIdAndJobId(
        userId: UUID,
        jobId: UUID,
    ): UserJobEntity?

    @Query("SELECT uj FROM UserJobEntity uj JOIN FETCH uj.job WHERE uj.user.id = :userId AND uj.job.id IN :jobIds")
    fun findByUserIdAndJobIdIn(
        userId: UUID,
        jobIds: List<UUID>,
    ): List<UserJobEntity>
}
