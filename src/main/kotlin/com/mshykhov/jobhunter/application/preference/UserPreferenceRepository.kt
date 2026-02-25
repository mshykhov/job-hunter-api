package com.mshykhov.jobhunter.application.preference

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface UserPreferenceRepository : JpaRepository<UserPreferenceEntity, UUID> {
    fun findByUserId(userId: UUID): UserPreferenceEntity?

    @Query(
        "SELECT * FROM user_preferences WHERE NOT (:source = ANY(disabled_sources))",
        nativeQuery = true,
    )
    fun findBySourceAllowed(source: String): List<UserPreferenceEntity>
}
