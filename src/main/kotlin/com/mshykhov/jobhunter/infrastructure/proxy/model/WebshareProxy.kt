package com.mshykhov.jobhunter.infrastructure.proxy.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonIgnoreProperties(ignoreUnknown = true)
data class WebshareProxy(val proxyAddress: String, val port: Int, val username: String, val password: String, val countryCode: String, val valid: Boolean)
