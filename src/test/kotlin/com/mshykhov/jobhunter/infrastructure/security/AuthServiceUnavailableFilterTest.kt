package com.mshykhov.jobhunter.infrastructure.security

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.AuthenticationServiceException
import kotlin.test.assertEquals

class AuthServiceUnavailableFilterTest {
    private val objectMapper = ObjectMapper()
    private val filter = AuthServiceUnavailableFilter(objectMapper)

    @Nested
    inner class WhenAuthInfrastructureFails {
        @Test
        fun `should return 503 with JSON error body when chain throws AuthenticationServiceException`() {
            val request = MockHttpServletRequest()
            val response = MockHttpServletResponse()
            val chain =
                FilterChain { _, _ ->
                    throw AuthenticationServiceException("An error occurred while attempting to decode the Jwt")
                }

            filter.doFilter(request, response, chain)

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), response.status)
            assertEquals(MediaType.APPLICATION_JSON_VALUE, response.contentType)
            val body = objectMapper.readTree(response.contentAsByteArray)
            assertEquals("AUTH_SERVICE_UNAVAILABLE", body.get("code").asText())
        }
    }

    @Nested
    inner class WhenChainSucceeds {
        @Test
        fun `should pass through without altering response`() {
            val request = MockHttpServletRequest()
            val response = MockHttpServletResponse()
            var invoked = false
            val chain = FilterChain { _, _ -> invoked = true }

            filter.doFilter(request, response, chain)

            assertEquals(true, invoked)
            assertEquals(HttpStatus.OK.value(), response.status)
        }
    }

    @Nested
    inner class WhenChainThrowsOtherException {
        @Test
        fun `should not swallow non-authentication exceptions`() {
            val request = MockHttpServletRequest()
            val response = MockHttpServletResponse()
            val chain = FilterChain { _, _ -> throw IllegalStateException("boom") }

            assertThrows<IllegalStateException> {
                filter.doFilter(request, response, chain)
            }
        }
    }
}
