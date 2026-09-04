package com.mshykhov.jobhunter.api.rest.automation

import com.mshykhov.jobhunter.support.AbstractIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.time.Instant
import java.util.UUID

class AutomationWorkflowControllerIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `owner creates lists inspects pauses resumes and stops a run`() {
        mockMvc.put("/automation/delegation") { with(authentication(owner("write:automation"))) }.andExpect { status { isOk() } }
        val key = UUID.randomUUID()
        val result =
            mockMvc
                .post("/automation/workflows/runs") {
                    with(authentication(owner("write:automation")))
                    contentType = org.springframework.http.MediaType.APPLICATION_JSON
                    content = """{"idempotencyKey":"$key"}"""
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.status") { value("QUEUED") }
                    jsonPath("$.completedSteps") { value(0) }
                }.andReturn()
        val runId = com.fasterxml.jackson.databind.ObjectMapper().readTree(result.response.contentAsString)["id"].asText()

        mockMvc.get("/automation/workflows/runs") { with(authentication(owner("read:automation"))) }.andExpect {
            status { isOk() }
            jsonPath("$[0].id") { exists() }
        }
        mockMvc.get("/automation/workflows/runs/$runId") { with(authentication(owner("read:automation"))) }.andExpect {
            status { isOk() }
            jsonPath("$.events[0].eventType") { value("RUN_CREATED") }
        }
        mockMvc.post("/automation/workflows/runs/$runId/pause") { with(authentication(owner("write:automation"))) }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("PAUSED") }
        }
        mockMvc.post("/automation/workflows/runs/$runId/resume") { with(authentication(owner("write:automation"))) }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("QUEUED") }
        }
        mockMvc.post("/automation/workflows/runs/$runId/stop") { with(authentication(owner("write:automation"))) }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("STOPPED") }
        }
    }

    @Test
    fun `owner workflow endpoints reject non-owner missing scope and runner`() {
        mockMvc.get("/automation/workflows/runs") {
            with(authentication(jwt(OWNER_ISSUER, "another-user", "read:automation")))
        }.andExpect { status { isForbidden() } }
        mockMvc.get("/automation/workflows/runs") { with(authentication(owner())) }.andExpect { status { isForbidden() } }
        mockMvc.get("/automation/workflows/runs") { with(authentication(runner())) }.andExpect { status { isForbidden() } }
    }

    private fun owner(vararg scopes: String) = jwt(OWNER_ISSUER, OWNER_SUBJECT, *scopes)

    private fun runner() = jwt(RUNNER_ISSUER, "runner", "report:automation-health")

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
                .issuedAt(Instant.parse("2026-08-18T07:00:00Z"))
                .expiresAt(Instant.parse("2026-08-18T09:00:00Z"))
                .build()
        return JwtAuthenticationToken(jwt, scopes.map { SimpleGrantedAuthority("SCOPE_$it") })
    }

    private companion object {
        const val OWNER_ISSUER = "http://localhost/dev"
        const val OWNER_SUBJECT = "local-dev-user"
        const val RUNNER_ISSUER = "http://localhost/runner"
    }
}
