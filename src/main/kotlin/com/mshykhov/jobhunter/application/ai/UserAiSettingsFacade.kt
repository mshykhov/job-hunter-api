package com.mshykhov.jobhunter.application.ai

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
@Transactional(readOnly = true)
class UserAiSettingsFacade(
    private val userAiSettingsRepository: UserAiSettingsRepository,
) {
    fun findByUserId(userId: UUID): UserAiSettingsEntity? = userAiSettingsRepository.findByUserId(userId)

    @Transactional
    fun save(entity: UserAiSettingsEntity): UserAiSettingsEntity = userAiSettingsRepository.save(entity)
}
