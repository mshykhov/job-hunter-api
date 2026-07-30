package com.mshykhov.jobhunter.api.rest.settings.dto

import com.mshykhov.jobhunter.application.ai.UserAiProviderEntity
import com.mshykhov.jobhunter.application.common.ValidationException
import com.mshykhov.jobhunter.application.user.UserEntity
import jakarta.validation.Valid

data class SaveAiProviderChainRequest(
    @field:Valid
    val chain: List<SaveAiProviderChainEntryRequest>,
) {
    fun toEntities(user: UserEntity): List<UserAiProviderEntity> {
        validatePriorities()
        validateNoDuplicateProviders()
        return chain.map { it.toEntity(user) }
    }

    private fun validatePriorities() {
        val expected = (1..chain.size).toList()
        if (chain.map { it.priority }.sorted() != expected) {
            throw ValidationException("chain: priorities must be a contiguous ascending sequence starting at 1")
        }
    }

    private fun validateNoDuplicateProviders() {
        val providers = chain.map { it.provider }
        if (providers.size != providers.toSet().size) {
            throw ValidationException("chain: each provider may appear at most once")
        }
    }
}
