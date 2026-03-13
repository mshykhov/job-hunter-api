package com.mshykhov.jobhunter.api.rest.criteria.dto

import com.mshykhov.jobhunter.application.job.Category

data class SearchCriteriaResponse(val categories: Set<Category>, val locations: List<String>, val remoteOnly: Boolean)
