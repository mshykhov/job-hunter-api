package com.mshykhov.jobhunter.persistence.repository

import com.mshykhov.jobhunter.persistence.model.UserPreferenceEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface UserPreferenceRepository : JpaRepository<UserPreferenceEntity, UUID> {
    fun findByUserSub(userSub: String): UserPreferenceEntity?

    fun existsByUserSub(userSub: String): Boolean

    @Query(
        "SELECT * FROM user_preferences WHERE :source = ANY(enabled_sources)",
        nativeQuery = true,
    )
    fun findByEnabledSource(source: String): List<UserPreferenceEntity>
}
