package com.mshykhov.jobhunter.service

import com.mshykhov.jobhunter.controller.preference.dto.PreferenceResponse
import org.springframework.stereotype.Service

@Service
class PreferenceNormalizeService {
    fun normalize(rawInput: String): PreferenceResponse =
        PreferenceResponse(
            rawInput = rawInput,
            categories = emptyList(),
            seniorityLevels = emptyList(),
            keywords = emptyList(),
            excludedKeywords = emptyList(),
            minSalary = null,
            remoteOnly = false,
            enabledSources = emptyList(),
            notificationsEnabled = true,
        )
}
