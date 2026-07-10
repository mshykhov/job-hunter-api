package com.mshykhov.jobhunter.infrastructure.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.mshykhov.jobhunter.api.rest.exception.ErrorResponse
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Converts server-side authentication infrastructure failures (e.g. the Auth0 JWKS endpoint being
 * briefly unreachable) into an honest 503 instead of a bare 500. Spring Security rethrows
 * [AuthenticationServiceException] past the entry point, so it must be caught above the security
 * filter chain rather than in the controller advice.
 */
class AuthServiceUnavailableFilter(private val objectMapper: ObjectMapper) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            filterChain.doFilter(request, response)
        } catch (ex: AuthenticationServiceException) {
            logger.warn("Authentication service unavailable: ${ex.message}")
            response.status = HttpStatus.SERVICE_UNAVAILABLE.value()
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            objectMapper.writeValue(
                response.outputStream,
                ErrorResponse("Authentication temporarily unavailable", "AUTH_SERVICE_UNAVAILABLE"),
            )
        }
    }
}
