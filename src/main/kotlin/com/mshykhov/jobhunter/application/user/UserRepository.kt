package com.mshykhov.jobhunter.application.user

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserRepository : JpaRepository<UserEntity, UUID> {
    fun findByAuth0Sub(auth0Sub: String): UserEntity?
}
