package com.mshykhov.jobhunter.service

import com.mshykhov.jobhunter.model.dto.SearchCriteriaResponse
import com.mshykhov.jobhunter.persistence.facade.UserPreferenceFacade
import org.springframework.stereotype.Service

@Service
class SearchCriteriaService(
    private val userPreferenceFacade: UserPreferenceFacade,
) {
    fun getAggregated(source: String): SearchCriteriaResponse {
        TODO("implement")
    }
}
