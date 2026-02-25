package com.mshykhov.jobhunter.application.preference

import com.mshykhov.jobhunter.application.preference.UserPreferenceEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface UserPreferenceRepository : JpaRepository<UserPreferenceEntity, UUID> {
    fun findByUserId(userId: UUID): UserPreferenceEntity?

    @Query(
        "SELECT * FROM user_preferences WHERE :source = ANY(enabled_sources)",
        nativeQuery = true,
    )
    fun findByEnabledSource(source: String): List<UserPreferenceEntity>
}
