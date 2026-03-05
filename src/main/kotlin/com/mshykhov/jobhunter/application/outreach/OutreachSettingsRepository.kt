package com.mshykhov.jobhunter.application.outreach

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface OutreachSettingsRepository : JpaRepository<OutreachSettingsEntity, UUID> {
    fun findByUserId(userId: UUID): OutreachSettingsEntity?
}
