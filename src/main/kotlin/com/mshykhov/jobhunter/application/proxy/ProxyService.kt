package com.mshykhov.jobhunter.application.proxy

import com.mshykhov.jobhunter.api.rest.exception.custom.ServiceUnavailableException
import com.mshykhov.jobhunter.api.rest.proxy.dto.ProxyResponse
import com.mshykhov.jobhunter.application.job.JobSource
import com.mshykhov.jobhunter.infrastructure.fingerprint.FingerprintProvider
import com.mshykhov.jobhunter.infrastructure.proxy.WebshareClient
import com.mshykhov.jobhunter.infrastructure.proxy.WebshareProperties
import com.mshykhov.jobhunter.infrastructure.proxy.WebshareProxy
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger {}

@Service
class ProxyService(
    private val webshareClientProvider: ObjectProvider<WebshareClient>,
    private val fingerprintProvider: ObjectProvider<FingerprintProvider>,
    private val proxyProperties: WebshareProperties,
) {
    @Volatile
    private var cachedProxies: List<WebshareProxy> = emptyList()

    @Volatile
    private var lastRefresh: Instant = Instant.MIN

    private val counters = ConcurrentHashMap<JobSource, AtomicInteger>()

    fun getProxy(source: JobSource): ProxyResponse {
        val proxies = getValidProxies()
        val counter = counters.computeIfAbsent(source) { AtomicInteger(0) }
        val index = Math.floorMod(counter.getAndIncrement(), proxies.size)
        return toResponse(proxies[index])
    }

    fun getAllProxies(): List<ProxyResponse> = getValidProxies().map { toResponse(it) }

    private fun toResponse(proxy: WebshareProxy): ProxyResponse {
        val provider = fingerprintProvider.ifAvailable
        val fingerprint = provider?.getFingerprintForProxy("${proxy.proxyAddress}:${proxy.port}")
        return ProxyResponse.from(proxy, fingerprint)
    }

    private fun getValidProxies(): List<WebshareProxy> {
        val client =
            webshareClientProvider.ifAvailable
                ?: throw ServiceUnavailableException("Proxy service is not configured")

        if (needsRefresh()) {
            refresh(client)
        }

        val proxies = cachedProxies
        if (proxies.isEmpty()) {
            throw ServiceUnavailableException("No proxies available")
        }
        return proxies
    }

    private fun needsRefresh(): Boolean = Duration.between(lastRefresh, Instant.now()).toMinutes() >= proxyProperties.cacheTtlMinutes

    @Synchronized
    private fun refresh(client: WebshareClient) {
        if (!needsRefresh()) return
        try {
            cachedProxies = client.fetchProxies()
            lastRefresh = Instant.now()
        } catch (e: Exception) {
            logger.error(e) { "Failed to refresh proxy list from Webshare" }
            if (cachedProxies.isEmpty()) {
                throw ServiceUnavailableException("Failed to fetch proxies from Webshare")
            }
        }
    }
}
