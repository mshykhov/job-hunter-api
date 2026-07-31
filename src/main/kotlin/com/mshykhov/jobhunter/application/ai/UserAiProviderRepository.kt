package com.mshykhov.jobhunter.application.ai

import com.mshykhov.jobhunter.application.settings.AiProvider
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserAiProviderRepository : JpaRepository<UserAiProviderEntity, UUID> {
    fun findByUserIdOrderByPriorityAsc(userId: UUID): List<UserAiProviderEntity>

    fun findByUserIdAndProvider(
        userId: UUID,
        provider: AiProvider,
    ): UserAiProviderEntity?

    fun deleteByUserId(userId: UUID)
}
