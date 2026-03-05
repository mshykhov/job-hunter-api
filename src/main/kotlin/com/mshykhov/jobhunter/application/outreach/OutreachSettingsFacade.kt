package com.mshykhov.jobhunter.application.outreach

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
@Transactional(readOnly = true)
class OutreachSettingsFacade(
    private val outreachSettingsRepository: OutreachSettingsRepository,
) {
    fun findByUserId(userId: UUID): OutreachSettingsEntity? = outreachSettingsRepository.findByUserId(userId)

    @Transactional
    fun save(entity: OutreachSettingsEntity): OutreachSettingsEntity = outreachSettingsRepository.save(entity)
}
