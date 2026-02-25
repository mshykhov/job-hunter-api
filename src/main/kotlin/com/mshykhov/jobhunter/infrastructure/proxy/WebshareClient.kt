package com.mshykhov.jobhunter.infrastructure.proxy

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.web.client.RestClient

private val logger = KotlinLogging.logger {}

class WebshareClient(
    private val restClient: RestClient,
) {
    fun fetchProxies(): List<WebshareProxy> {
        val proxies = mutableListOf<WebshareProxy>()
        var page = 1

        do {
            val response =
                restClient
                    .get()
                    .uri("/proxy/list/?mode=direct&page={page}&page_size={pageSize}", page, PAGE_SIZE)
                    .retrieve()
                    .body(WebshareProxyListResponse::class.java)
                    ?: break

            proxies.addAll(response.results.filter { it.valid })
            page++
        } while (response.next != null)

        logger.info { "Fetched ${proxies.size} valid proxies from Webshare" }
        return proxies
    }

    companion object {
        private const val PAGE_SIZE = 100
    }
}

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonIgnoreProperties(ignoreUnknown = true)
data class WebshareProxyListResponse(
    val count: Int,
    val next: String?,
    val results: List<WebshareProxy>,
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonIgnoreProperties(ignoreUnknown = true)
data class WebshareProxy(
    val proxyAddress: String,
    val port: Int,
    val username: String,
    val password: String,
    val countryCode: String,
    val valid: Boolean,
)
