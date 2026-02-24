package com.mshykhov.jobhunter.service

import com.mshykhov.jobhunter.controller.criteria.dto.SearchCriteriaResponse
import com.mshykhov.jobhunter.persistence.facade.UserPreferenceFacade
import com.mshykhov.jobhunter.persistence.model.JobSource
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
