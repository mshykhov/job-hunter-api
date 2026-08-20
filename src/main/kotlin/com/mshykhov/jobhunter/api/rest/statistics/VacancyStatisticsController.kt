package com.mshykhov.jobhunter.api.rest.statistics

import com.mshykhov.jobhunter.api.rest.statistics.dto.VacancyStatisticsQueryRequest
import com.mshykhov.jobhunter.api.rest.statistics.dto.VacancyStatisticsResponse
import com.mshykhov.jobhunter.application.statistics.VacancyStatisticsService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/statistics/vacancies")
class VacancyStatisticsController(private val service: VacancyStatisticsService) {
    @PostMapping("/query")
    @PreAuthorize("hasAuthority('SCOPE_read:jobs')")
    fun query(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody request: VacancyStatisticsQueryRequest,
    ): VacancyStatisticsResponse = VacancyStatisticsResponse.from(service.query(jwt.subject, request.toQuery()))
}
