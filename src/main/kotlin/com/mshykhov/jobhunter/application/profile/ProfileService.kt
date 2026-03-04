package com.mshykhov.jobhunter.application.profile

import com.mshykhov.jobhunter.api.rest.profile.dto.ProfileResponse
import com.mshykhov.jobhunter.api.rest.profile.dto.ProfileWarning
import com.mshykhov.jobhunter.application.ai.UserAiSettingsFacade
import com.mshykhov.jobhunter.application.common.NotFoundException
import com.mshykhov.jobhunter.application.preference.UserPreferenceFacade
import com.mshykhov.jobhunter.application.user.UserFacade
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProfileService(
    private val userFacade: UserFacade,
    private val userAiSettingsFacade: UserAiSettingsFacade,
    private val userPreferenceFacade: UserPreferenceFacade,
) {
    @Transactional(readOnly = true)
    fun get(auth0Sub: String): ProfileResponse {
        val user =
            userFacade.findByAuth0Sub(auth0Sub)
                ?: throw NotFoundException("User not found")

        val hasAiSettings = userAiSettingsFacade.findByUserId(user.id) != null
        val preference = userPreferenceFacade.findByUserId(user.id)

        val warnings =
            buildList {
                if (!hasAiSettings) add(ProfileWarning.AI_NOT_CONFIGURED)
                if (preference == null) add(ProfileWarning.PREFERENCES_NOT_SET)
            }

        return ProfileResponse(
            aiConfigured = hasAiSettings,
            preferencesConfigured = preference != null,
            telegramConfigured = preference?.telegram?.chatId != null,
            warnings = warnings,
        )
    }
}
