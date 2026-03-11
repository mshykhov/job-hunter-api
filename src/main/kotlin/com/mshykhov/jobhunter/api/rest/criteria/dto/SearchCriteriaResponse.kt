package com.mshykhov.jobhunter.api.rest.criteria.dto

data class SearchCriteriaResponse(val categories: List<String>, val locations: List<String>, val remoteOnly: Boolean)
