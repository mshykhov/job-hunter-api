package com.mshykhov.jobhunter.application.job

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface JobGroupRepository : JpaRepository<JobGroupEntity, UUID> {
    fun findByGroupKey(groupKey: String): JobGroupEntity?

    fun findByGroupKeyIn(groupKeys: List<String>): List<JobGroupEntity>

    @Modifying(clearAutomatically = true)
    @Query("UPDATE JobGroupEntity g SET g.jobCount = g.jobCount + 1 WHERE g.id = :id")
    fun incrementJobCount(id: UUID)

    @Modifying
    @Query(
        nativeQuery = true,
        value = """
            INSERT INTO job_groups (id, group_key, title, company, job_count, created_at, updated_at)
            VALUES (:id, :groupKey, :title, :company, 1, now(), now())
            ON CONFLICT (group_key) DO UPDATE SET updated_at = now()
        """,
    )
    fun upsert(
        id: UUID,
        groupKey: String,
        title: String,
        company: String?,
    )
}
