package com.mshykhov.jobhunter.application.ai

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
@Transactional(readOnly = true)
class UserAiProviderFacade(private val userAiProviderRepository: UserAiProviderRepository) {
    fun findByUserId(userId: UUID): List<UserAiProviderEntity> = userAiProviderRepository.findByUserIdOrderByPriorityAsc(userId)

    fun findByUserIdAndProvider(
        userId: UUID,
        provider: String,
    ): UserAiProviderEntity? = userAiProviderRepository.findByUserIdAndProvider(userId, provider)

    @Transactional
    fun saveAll(entities: List<UserAiProviderEntity>): List<UserAiProviderEntity> = userAiProviderRepository.saveAll(entities)

    @Transactional
    fun deleteAll(userId: UUID) = userAiProviderRepository.deleteByUserId(userId)
}
