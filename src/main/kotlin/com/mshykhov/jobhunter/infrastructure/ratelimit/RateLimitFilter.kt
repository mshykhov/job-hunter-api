package com.mshykhov.jobhunter.infrastructure.ratelimit

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.benmanes.caffeine.cache.Caffeine
import com.mshykhov.jobhunter.api.rest.exception.ErrorResponse
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.filter.OncePerRequestFilter
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class RateLimitFilter(private val properties: RateLimitProperties, private val objectMapper: ObjectMapper) : OncePerRequestFilter() {
    private val requestCounts =
        Caffeine
            .newBuilder()
            .expireAfterWrite(properties.windowSeconds, TimeUnit.SECONDS)
            .maximumSize(properties.maxCacheSize)
            .build<String, AtomicInteger>()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val clientIp = resolveClientIp(request)
        val counter = requestCounts.get(clientIp) { AtomicInteger(0) }
        val currentCount = counter.incrementAndGet()

        response.setHeader(HEADER_LIMIT, properties.maxRequests.toString())
        response.setHeader(HEADER_REMAINING, (properties.maxRequests - currentCount).coerceAtLeast(0).toString())

        if (currentCount > properties.maxRequests) {
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.setHeader(HEADER_RETRY_AFTER, properties.windowSeconds.toString())
            objectMapper.writeValue(response.writer, ErrorResponse("Rate limit exceeded", "RATE_LIMITED"))
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun resolveClientIp(request: HttpServletRequest): String =
        request
            .getHeader("X-Forwarded-For")
            ?.split(",")
            ?.firstOrNull()
            ?.trim()
            ?: request.remoteAddr

    companion object {
        private const val HEADER_LIMIT = "X-RateLimit-Limit"
        private const val HEADER_REMAINING = "X-RateLimit-Remaining"
        private const val HEADER_RETRY_AFTER = "Retry-After"
    }
}
