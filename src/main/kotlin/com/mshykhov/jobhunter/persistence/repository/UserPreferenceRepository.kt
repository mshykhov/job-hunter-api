package com.mshykhov.jobhunter.persistence.repository

import com.mshykhov.jobhunter.persistence.model.UserPreferenceEntity
import org.springframework.data.jpa.repository.JpaRepository

interface UserPreferenceRepository : JpaRepository<UserPreferenceEntity, Long> {
    fun findByUserSub(userSub: String): UserPreferenceEntity?

    fun existsByUserSub(userSub: String): Boolean
}
