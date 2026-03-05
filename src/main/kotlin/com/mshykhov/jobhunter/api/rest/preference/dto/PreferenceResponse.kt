package com.mshykhov.jobhunter.api.rest.preference.dto

import com.mshykhov.jobhunter.application.preference.UserPreferenceEntity

data class PreferenceResponse(
    val about: String?,
    val search: SearchPreferenceResponse,
    val matching: MatchingPreferenceResponse,
    val telegram: TelegramPreferenceResponse,
) {
    companion object {
        fun from(entity: UserPreferenceEntity): PreferenceResponse =
            PreferenceResponse(
                about = entity.about,
                search = SearchPreferenceResponse.from(entity.search),
                matching = MatchingPreferenceResponse.from(entity.matching),
                telegram = TelegramPreferenceResponse.from(entity.telegram),
            )
    }
}
