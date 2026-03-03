package com.mshykhov.jobhunter.infrastructure.config

import com.github.benmanes.caffeine.cache.Caffeine
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@Component
class RateLimitFilter : OncePerRequestFilter() {
    private val requestCounts =
        Caffeine
            .newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build<String, AtomicInteger>()

    override fun shouldNotFilter(request: HttpServletRequest): Boolean = !request.requestURI.startsWith("/public/")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val clientIp = resolveClientIp(request)
        val counter = requestCounts.get(clientIp) { AtomicInteger(0) }
        val currentCount = counter.incrementAndGet()

        response.setHeader(HEADER_LIMIT, MAX_REQUESTS.toString())
        response.setHeader(HEADER_REMAINING, (MAX_REQUESTS - currentCount).coerceAtLeast(0).toString())

        if (currentCount > MAX_REQUESTS) {
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.setHeader(HEADER_RETRY_AFTER, "60")
            response.writer.write("""{"message":"Rate limit exceeded","code":"RATE_LIMITED"}""")
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
        private const val MAX_REQUESTS = 60
        private const val HEADER_LIMIT = "X-RateLimit-Limit"
        private const val HEADER_REMAINING = "X-RateLimit-Remaining"
        private const val HEADER_RETRY_AFTER = "Retry-After"
    }
}
