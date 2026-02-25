package com.mshykhov.jobhunter.persistence.facade

import com.mshykhov.jobhunter.persistence.model.UserPreferenceEntity
import com.mshykhov.jobhunter.persistence.repository.UserPreferenceRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
@Transactional(readOnly = true)
class UserPreferenceFacade(
    private val userPreferenceRepository: UserPreferenceRepository,
) {
    fun findByEnabledSource(source: String): List<UserPreferenceEntity> = userPreferenceRepository.findByEnabledSource(source)

    fun findByUserId(userId: UUID): UserPreferenceEntity? = userPreferenceRepository.findByUserId(userId)

    @Transactional
    fun save(entity: UserPreferenceEntity): UserPreferenceEntity = userPreferenceRepository.save(entity)
}
