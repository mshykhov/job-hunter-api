package com.mshykhov.jobhunter.infrastructure.proxy

import com.mshykhov.jobhunter.infrastructure.proxy.model.WebshareProxy
import com.mshykhov.jobhunter.infrastructure.proxy.model.WebshareProxyListResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

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
                    .body<WebshareProxyListResponse>()
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
