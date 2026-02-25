package com.mshykhov.jobhunter.application.user

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

    @Transactional
    fun findOrCreate(auth0Sub: String): UserEntity =
        userRepository.findByAuth0Sub(auth0Sub)
            ?: userRepository.save(UserEntity(auth0Sub = auth0Sub))
}
