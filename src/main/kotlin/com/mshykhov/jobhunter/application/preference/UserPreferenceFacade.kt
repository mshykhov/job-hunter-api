package com.mshykhov.jobhunter.application.preference

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
@Transactional(readOnly = true)
class UserPreferenceFacade(
    private val userPreferenceRepository: UserPreferenceRepository,
) {
    fun findAll(): List<UserPreferenceEntity> = userPreferenceRepository.findAll()

    fun findBySourceAllowed(source: String): List<UserPreferenceEntity> = userPreferenceRepository.findBySourceAllowed(source)

    fun findByUserId(userId: UUID): UserPreferenceEntity? = userPreferenceRepository.findByUserId(userId)

    @Transactional
    fun save(entity: UserPreferenceEntity): UserPreferenceEntity = userPreferenceRepository.save(entity)
}
