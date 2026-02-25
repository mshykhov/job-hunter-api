package com.mshykhov.jobhunter.application.user

import com.mshykhov.jobhunter.application.user.UserEntity
import com.mshykhov.jobhunter.application.user.UserRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional(readOnly = true)
class UserFacade(
    private val userRepository: UserRepository,
) {
    fun findByAuth0Sub(auth0Sub: String): UserEntity? = userRepository.findByAuth0Sub(auth0Sub)

    @Transactional
    fun save(entity: UserEntity): UserEntity = userRepository.save(entity)
}
