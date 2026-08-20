package com.mshykhov.jobhunter.api.rest.statistics

import com.mshykhov.jobhunter.application.user.UserEntity
import com.mshykhov.jobhunter.application.user.UserRepository
import com.mshykhov.jobhunter.support.AbstractIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.time.Instant

class VacancyStatisticsControllerIntegrationTest : AbstractIntegrationTest() {
    @Autowired lateinit var mockMvc: MockMvc

    @Autowired lateinit var userRepository: UserRepository

    @Test
    fun `should expose protected statistics without api prefix and validate range`() {
        val subject = "statistics-controller-${System.nanoTime()}"
        userRepository.save(UserEntity(auth0Sub = subject))

        mockMvc.post("/statistics/vacancies/query") {
            with(authentication(jwt(subject, "read:jobs")))
            contentType = APPLICATION_JSON
            content = """{"from":"2092-01-02T00:00:00Z","to":"2092-01-01T00:00:00Z"}"""
        }.andExpect { status { isBadRequest() } }

        mockMvc.post("/api/statistics/vacancies/query") {
            with(authentication(jwt(subject, "read:jobs")))
            contentType = APPLICATION_JSON
            content = "{}"
        }.andExpect { status { isNotFound() } }
    }

    @Test
    fun `should reject invalid bucket json`() {
        mockMvc.post("/statistics/vacancies/query") {
            with(authentication(jwt("invalid-bucket-${System.nanoTime()}", "read:jobs")))
            contentType = APPLICATION_JSON
            content = """{"bucket":"YEAR"}"""
        }.andExpect { status { isBadRequest() } }
    }

    private fun jwt(subject: String, vararg scopes: String): JwtAuthenticationToken {
        val jwt =
            Jwt
                .withTokenValue("test")
                .header("alg", "none")
                .issuer("http://localhost/dev")
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build()
        return JwtAuthenticationToken(jwt, scopes.map { SimpleGrantedAuthority("SCOPE_$it") })
    }
}
