package com.mshykhov.jobhunter.controller

import com.mshykhov.jobhunter.model.dto.SearchCriteriaResponse
import com.mshykhov.jobhunter.service.SearchCriteriaService
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
        @RequestParam source: String,
    ): SearchCriteriaResponse = searchCriteriaService.getAggregated(source)
}
