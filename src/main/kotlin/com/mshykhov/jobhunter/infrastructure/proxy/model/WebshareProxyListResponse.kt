package com.mshykhov.jobhunter.infrastructure.proxy.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonIgnoreProperties(ignoreUnknown = true)
data class WebshareProxyListResponse(
    val count: Int,
    val next: String?,
    val results: List<WebshareProxy>,
)
