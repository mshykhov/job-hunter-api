package com.mshykhov.jobhunter.api.rest.criteria

import com.mshykhov.jobhunter.api.rest.criteria.dto.SearchCriteriaResponse
import com.mshykhov.jobhunter.application.criteria.SearchCriteriaService
import com.mshykhov.jobhunter.application.job.JobSource
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/criteria")
class SearchCriteriaController(
    private val searchCriteriaService: SearchCriteriaService,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_read:criteria')")
    fun get(
        @RequestParam source: JobSource,
    ): SearchCriteriaResponse = searchCriteriaService.getAggregated(source)
}
