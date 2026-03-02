package com.mshykhov.jobhunter.infrastructure.fingerprint

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.web.client.RestClient
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

class FingerprintProvider(
    private val restClient: RestClient,
    private val properties: ScrapeOpsProperties,
) {
    private val cache = AtomicReference<List<BrowserFingerprint>>(emptyList())

    @Volatile
    private var lastRefresh: Instant = Instant.EPOCH

    private val proxyFingerprints = ConcurrentHashMap<String, BrowserFingerprint>()

    fun getFingerprintForProxy(proxyKey: String): BrowserFingerprint {
        refreshIfNeeded()

        val fingerprints = cache.get()
        if (fingerprints.isEmpty()) {
            return BrowserFingerprint.FALLBACK
        }

        return proxyFingerprints.computeIfAbsent(proxyKey) {
            fingerprints[Random.nextInt(fingerprints.size)].also {
                logger.debug { "Assigned fingerprint to $proxyKey: ${it.userAgent?.take(50)}" }
            }
        }
    }

    private fun refreshIfNeeded() {
        val now = Instant.now()
        val refreshInterval = Duration.ofMinutes(properties.refreshIntervalMinutes)
        if (Duration.between(lastRefresh, now) < refreshInterval && cache.get().isNotEmpty()) {
            return
        }

        synchronized(this) {
            if (Duration.between(lastRefresh, now) < refreshInterval && cache.get().isNotEmpty()) {
                return
            }
            refresh()
        }
    }

    private fun refresh() {
        try {
            val response =
                restClient
                    .get()
                    .uri("/browser-headers?api_key={apiKey}&num_results={num}", properties.apiKey, properties.numResults)
                    .retrieve()
                    .body(ScrapeOpsResponse::class.java)

            if (response != null && response.result.isNotEmpty()) {
                cache.set(response.result)
                lastRefresh = Instant.now()
                proxyFingerprints.clear()
                logger.info { "Refreshed browser fingerprints: ${response.result.size} variants" }
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to refresh fingerprints from ScrapeOps" }
        }
    }
}
