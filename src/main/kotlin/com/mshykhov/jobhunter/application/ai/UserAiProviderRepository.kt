package com.mshykhov.jobhunter.application.ai

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserAiProviderRepository : JpaRepository<UserAiProviderEntity, UUID> {
    fun findByUserIdOrderByPriorityAsc(userId: UUID): List<UserAiProviderEntity>

    fun findByUserIdAndProvider(
        userId: UUID,
        provider: String,
    ): UserAiProviderEntity?

    fun deleteByUserId(userId: UUID)
}
