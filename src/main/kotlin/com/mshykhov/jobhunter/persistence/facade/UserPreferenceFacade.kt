package com.mshykhov.jobhunter.persistence.facade

import com.mshykhov.jobhunter.persistence.repository.UserPreferenceRepository
import org.springframework.stereotype.Component

@Component
class UserPreferenceFacade(
    private val userPreferenceRepository: UserPreferenceRepository,
)
