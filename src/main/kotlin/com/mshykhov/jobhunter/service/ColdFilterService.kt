package com.mshykhov.jobhunter.service

import com.mshykhov.jobhunter.persistence.model.JobEntity
import com.mshykhov.jobhunter.persistence.model.UserPreferenceEntity
import org.springframework.stereotype.Service

@Service
class ColdFilterService {
    fun matches(
        job: JobEntity,
        preference: UserPreferenceEntity,
    ): Boolean =
        isSourceEnabled(job, preference) &&
            isRemoteMatch(job, preference) &&
            hasNoExcludedKeywords(job, preference) &&
            matchesCategories(job, preference)

    private fun isSourceEnabled(
        job: JobEntity,
        preference: UserPreferenceEntity,
    ): Boolean {
        if (preference.enabledSources.isEmpty()) return true
        return job.source.name in preference.enabledSources
    }

    private fun isRemoteMatch(
        job: JobEntity,
        preference: UserPreferenceEntity,
    ): Boolean {
        if (!preference.remoteOnly) return true
        return job.remote
    }

    private fun hasNoExcludedKeywords(
        job: JobEntity,
        preference: UserPreferenceEntity,
    ): Boolean {
        if (preference.excludedKeywords.isEmpty()) return true
        val searchText = "${job.title} ${job.description}".lowercase()
        return preference.excludedKeywords.none { keyword ->
            searchText.contains(keyword.lowercase())
        }
    }

    private fun matchesCategories(
        job: JobEntity,
        preference: UserPreferenceEntity,
    ): Boolean {
        if (preference.categories.isEmpty()) return true
        val searchText = "${job.title} ${job.description}".lowercase()
        return preference.categories.any { category ->
            searchText.contains(category.lowercase())
        }
    }
}
