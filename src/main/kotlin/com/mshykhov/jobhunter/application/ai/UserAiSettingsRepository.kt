package com.mshykhov.jobhunter.application.ai

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserAiSettingsRepository : JpaRepository<UserAiSettingsEntity, UUID> {
    fun findByUserId(userId: UUID): UserAiSettingsEntity?
}
