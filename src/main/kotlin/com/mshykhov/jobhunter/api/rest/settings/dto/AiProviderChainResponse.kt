package com.mshykhov.jobhunter.api.rest.settings.dto

import com.mshykhov.jobhunter.application.ai.UserAiProviderEntity

data class AiProviderChainResponse(val chain: List<AiProviderChainEntryResponse>) {
    companion object {
        fun from(entities: List<UserAiProviderEntity>): AiProviderChainResponse =
            AiProviderChainResponse(entities.sortedBy { it.priority }.map { AiProviderChainEntryResponse.from(it) })
    }
}
