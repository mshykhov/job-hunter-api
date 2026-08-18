package com.mshykhov.jobhunter.api.rest.automation

import com.mshykhov.jobhunter.support.AbstractIntegrationTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.time.Instant

class AutomationRunnerControllerIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var mockMvc: MockMvc

    @BeforeEach
    fun enableDelegation() {
        mockMvc.put("/automation/delegation") { with(authentication(owner())) }.andExpect { status { isOk() } }
    }

    @Test
    fun `runner starts a fenced session`() {
        mockMvc.post("/automation/runner/session") { with(authentication(runner())) }.andExpect {
            status { isOk() }
            jsonPath("$.runnerKey") { value("primary") }
            jsonPath("$.generation") { isNumber() }
            jsonPath("$.heartbeatIntervalSeconds") { value(60) }
            jsonPath("$.preflightIntervalSeconds") { value(300) }
            jsonPath("$.codexCanaryIntervalSeconds") { value(21600) }
        }
    }

    @Test
    fun `owner and missing runner scope are forbidden`() {
        mockMvc.post("/automation/runner/session") { with(authentication(owner())) }.andExpect {
            status { isForbidden() }
        }
        mockMvc.post("/automation/runner/session") {
            with(authentication(jwt(RUNNER_ISSUER, "runner")))
        }.andExpect { status { isForbidden() } }
    }

    @Test
    fun `heartbeat rejects user selection and invalid fencing`() {
        val generation = startSession()
        val idempotencyKey = "d07cb2ae-3b18-46d4-8c2d-aeaaf3bbce1f"
        val valid = heartbeat(generation, 1, idempotencyKey)

        mockMvc.put("/automation/runner/heartbeat") {
            with(authentication(runner()))
            contentType = MediaType.APPLICATION_JSON
            content = valid.dropLast(1) + ",\"userId\":\"someone-else\"}"
        }.andExpect { status { isBadRequest() } }

        mockMvc.put("/automation/runner/heartbeat") {
            with(authentication(runner()))
            contentType = MediaType.APPLICATION_JSON
            content = heartbeat(generation - 1, 1, idempotencyKey)
        }.andExpect { status { isConflict() } }

        mockMvc.put("/automation/runner/heartbeat") {
            with(authentication(runner()))
            contentType = MediaType.APPLICATION_JSON
            content = heartbeat(generation, 1, idempotencyKey, "2026-08-18T06:00:00Z")
        }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `duplicate heartbeat is idempotent`() {
        val generation = startSession()
        val request = heartbeat(generation, 1, "a926efaf-496f-498b-bf69-62316a23aaf2")

        repeat(2) {
            mockMvc.put("/automation/runner/heartbeat") {
                with(authentication(runner()))
                contentType = MediaType.APPLICATION_JSON
                content = request
            }.andExpect {
                status { isOk() }
                jsonPath("$.acceptedSequence") { value(1) }
            }
        }
    }

    private fun startSession(): Long {
        val response =
            mockMvc
                .post("/automation/runner/session") { with(authentication(runner())) }
                .andExpect { status { isOk() } }
                .andReturn()
                .response.contentAsString
        return Regex("\"generation\":(\\d+)").find(response)?.groupValues?.get(1)?.toLong()
            ?: error("Missing generation")
    }

    private fun heartbeat(
        generation: Long,
        sequence: Long,
        idempotencyKey: String,
        sentAt: String = Instant.now().toString(),
    ): String =
        """
        {
          "generation": $generation,
          "sequence": $sequence,
          "idempotencyKey": "$idempotencyKey",
          "sentAt": "$sentAt",
          "launcherVersion": "0.1.0",
          "components": {}
        }
        """.trimIndent()

    private fun owner(): JwtAuthenticationToken = jwt(OWNER_ISSUER, OWNER_SUBJECT, "write:automation")

    private fun runner(): JwtAuthenticationToken = jwt(RUNNER_ISSUER, "runner", "report:automation-health")

    private fun jwt(
        issuer: String,
        subject: String,
        vararg scopes: String,
    ): JwtAuthenticationToken {
        val jwt =
            Jwt
                .withTokenValue("test-token")
                .header("alg", "none")
                .issuer(issuer)
                .subject(subject)
                .claim("permissions", scopes.toList())
                .issuedAt(Instant.now().minusSeconds(60))
                .expiresAt(Instant.now().plusSeconds(3600))
                .build()
        return JwtAuthenticationToken(jwt, scopes.map { SimpleGrantedAuthority("SCOPE_$it") })
    }

    private companion object {
        const val OWNER_ISSUER = "http://localhost/dev"
        const val OWNER_SUBJECT = "local-dev-user"
        const val RUNNER_ISSUER = "http://localhost/runner"
    }
}
