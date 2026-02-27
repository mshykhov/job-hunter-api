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
        val preferences = userPreferenceFacade.findBySourceAllowed(source.name)
        val categories = preferences.flatMap { it.categories }.distinct()
        val locations = preferences.flatMap { it.locations }.distinct()
        val remoteOnly = preferences.isNotEmpty() && preferences.all { it.remoteOnly }
        return SearchCriteriaResponse(categories = categories, locations = locations, remoteOnly = remoteOnly)
    }
}
