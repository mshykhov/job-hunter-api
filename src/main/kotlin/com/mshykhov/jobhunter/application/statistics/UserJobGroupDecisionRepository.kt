package com.mshykhov.jobhunter.application.statistics

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserJobGroupDecisionRepository : JpaRepository<UserJobGroupDecisionEntity, UUID> {
    fun findByUserIdAndGroupId(userId: UUID, groupId: UUID): UserJobGroupDecisionEntity?
}
