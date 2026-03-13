package com.mshykhov.jobhunter.application.userjob

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface UserJobGroupRepository :
    JpaRepository<UserJobGroupEntity, UUID>,
    JpaSpecificationExecutor<UserJobGroupEntity> {
    @EntityGraph(attributePaths = ["group", "group.jobs"])
    @Query("SELECT ujg FROM UserJobGroupEntity ujg WHERE ujg.user.id = :userId AND ujg.group.id = :groupId")
    fun findByUserIdAndGroupId(
        userId: UUID,
        groupId: UUID,
    ): UserJobGroupEntity?

    @Query("SELECT ujg FROM UserJobGroupEntity ujg JOIN FETCH ujg.user WHERE ujg.group.id = :groupId")
    fun findByGroupId(groupId: UUID): List<UserJobGroupEntity>

    @EntityGraph(attributePaths = ["group", "group.jobs"])
    @Query("SELECT ujg FROM UserJobGroupEntity ujg JOIN ujg.group.jobs j WHERE ujg.user.id = :userId AND j.id = :jobId")
    fun findByUserIdAndJobId(
        userId: UUID,
        jobId: UUID,
    ): UserJobGroupEntity?

    @EntityGraph(attributePaths = ["group", "group.jobs"])
    @Query("SELECT ujg FROM UserJobGroupEntity ujg WHERE ujg.user.id = :userId AND ujg.group.id IN :groupIds")
    fun findByUserIdAndGroupIdIn(
        userId: UUID,
        groupIds: List<UUID>,
    ): List<UserJobGroupEntity>
}
