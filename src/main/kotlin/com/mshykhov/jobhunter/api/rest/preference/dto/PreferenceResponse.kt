package com.mshykhov.jobhunter.api.rest.preference.dto

import com.mshykhov.jobhunter.application.preference.UserPreferenceEntity

data class PreferenceResponse(
    val search: SearchPreferenceResponse,
    val matching: MatchingPreferenceResponse,
    val telegram: TelegramPreferenceResponse,
) {
    companion object {
        fun from(entity: UserPreferenceEntity): PreferenceResponse =
            PreferenceResponse(
                search = SearchPreferenceResponse.from(entity.search),
                matching = MatchingPreferenceResponse.from(entity.matching),
                telegram = TelegramPreferenceResponse.from(entity.telegram),
            )
    }
}
