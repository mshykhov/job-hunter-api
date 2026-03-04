package com.mshykhov.jobhunter.application.matching

import com.mshykhov.jobhunter.application.job.JobEntity
import com.mshykhov.jobhunter.application.preference.UserPreferenceEntity

internal class ColdFilterChain {
    fun evaluate(
        job: JobEntity,
        preference: UserPreferenceEntity,
    ): FilterResult {
        checkSource(job, preference)?.let { return it }
        checkRemote(job, preference)?.let { return it }
        checkExcludedKeywords(job, preference)?.let { return it }
        checkExcludedTitleKeywords(job, preference)?.let { return it }
        checkExcludedCompanies(job, preference)?.let { return it }
        checkCategories(job, preference)?.let { return it }
        return FilterResult.Passed
    }

    private fun checkSource(
        job: JobEntity,
        preference: UserPreferenceEntity,
    ): FilterResult.Rejected? {
        if (preference.search.disabledSources.isEmpty()) return null
        if (job.source in preference.search.disabledSources) {
            return FilterResult.Rejected("source", "source '${job.source}' is disabled")
        }
        return null
    }

    private fun checkRemote(
        job: JobEntity,
        preference: UserPreferenceEntity,
    ): FilterResult.Rejected? {
        if (!preference.search.remoteOnly) return null
        if (job.remote == false) {
            return FilterResult.Rejected("remote", "job is explicitly on-site")
        }
        return null
    }

    private fun checkExcludedKeywords(
        job: JobEntity,
        preference: UserPreferenceEntity,
    ): FilterResult.Rejected? {
        if (preference.matching.excludedKeywords.isEmpty()) return null
        val searchText = TextMatchUtils.buildSearchText(job)
        val hit =
            preference.matching.excludedKeywords.firstOrNull {
                TextMatchUtils.containsSubstring(searchText, it)
            }
        if (hit != null) {
            return FilterResult.Rejected("excludedKeyword", "contains excluded keyword '$hit'")
        }
        return null
    }

    private fun checkExcludedTitleKeywords(
        job: JobEntity,
        preference: UserPreferenceEntity,
    ): FilterResult.Rejected? {
        if (preference.matching.excludedTitleKeywords.isEmpty()) return null
        val title = job.title.lowercase()
        val hit =
            preference.matching.excludedTitleKeywords.firstOrNull {
                TextMatchUtils.containsSubstring(title, it)
            }
        if (hit != null) {
            return FilterResult.Rejected("excludedTitleKeyword", "title contains '$hit'")
        }
        return null
    }

    private fun checkExcludedCompanies(
        job: JobEntity,
        preference: UserPreferenceEntity,
    ): FilterResult.Rejected? {
        if (preference.matching.excludedCompanies.isEmpty()) return null
        val company = job.company?.lowercase() ?: return null
        val hit =
            preference.matching.excludedCompanies.firstOrNull {
                TextMatchUtils.containsSubstring(company, it)
            }
        if (hit != null) {
            return FilterResult.Rejected("excludedCompany", "company matches '$hit'")
        }
        return null
    }

    private fun checkCategories(
        job: JobEntity,
        preference: UserPreferenceEntity,
    ): FilterResult.Rejected? {
        if (preference.search.categories.isEmpty()) return null
        val searchText = TextMatchUtils.buildSearchText(job)
        val matched = preference.search.categories.any { TextMatchUtils.containsWord(searchText, it) }
        if (!matched) {
            return FilterResult.Rejected("category", "no category matched ${preference.search.categories}")
        }
        return null
    }
}
