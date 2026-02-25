package com.mshykhov.jobhunter.application.criteria

import com.mshykhov.jobhunter.api.rest.criteria.dto.SearchCriteriaResponse
import com.mshykhov.jobhunter.application.job.JobSource
import com.mshykhov.jobhunter.application.preference.UserPreferenceFacade
import org.springframework.stereotype.Service

@Service
class SearchCriteriaService(
    private val userPreferenceFacade: UserPreferenceFacade,
) {
    fun getAggregated(source: JobSource): SearchCriteriaResponse {
        val preferences = userPreferenceFacade.findByEnabledSource(source.name)
        val categories = preferences.flatMap { it.categories }.distinct()
        return SearchCriteriaResponse(categories = categories)
    }
}
