package com.mshykhov.jobhunter.persistence.repository

import com.mshykhov.jobhunter.persistence.model.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserRepository : JpaRepository<UserEntity, UUID> {
    fun findByAuth0Sub(auth0Sub: String): UserEntity?
}
