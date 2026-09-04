package com.mshykhov.jobhunter.api.rest.automation

import com.mshykhov.jobhunter.application.automation.AutomationService
import com.mshykhov.jobhunter.application.automation.workflow.AutomationWorkflowService
import com.mshykhov.jobhunter.support.AbstractIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.time.Instant
import java.util.UUID

class AutomationWorkflowRunnerControllerIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var automationService: AutomationService

    @Autowired
    lateinit var workflowService: AutomationWorkflowService

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `runner claims and checkpoints synthetic work while owner token is rejected`() {
        jdbcTemplate.update("DELETE FROM automation_workflow_events")
        jdbcTemplate.update("DELETE FROM automation_workflow_checkpoints")
        jdbcTemplate.update("DELETE FROM automation_work_attempts")
        jdbcTemplate.update("DELETE FROM automation_work_items")
        jdbcTemplate.update("DELETE FROM automation_workflow_runs")
        automationService.enableDelegation()
        val generation = automationService.startSession().generation
        workflowService.createRun(UUID.randomUUID())
        val claimResult =
            mockMvc
                .post("/automation/runner/work-items/claims") {
                    with(authentication(runner()))
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"workerId":"http-worker","generation":$generation}"""
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.nextStepIndex") { value(0) }
                    jsonPath("$.steps[0]") { value("PREPARE") }
                }.andReturn()
        val claim = com.fasterxml.jackson.databind.ObjectMapper().readTree(claimResult.response.contentAsString)
        val itemId = claim["workItemId"].asText()
        val attemptId = claim["attemptId"].asText()
        val token = claim["leaseToken"].asText()

        mockMvc.post("/automation/runner/work-items/$itemId/checkpoints") {
            with(authentication(runner()))
            contentType = MediaType.APPLICATION_JSON
            content =
                """{"attemptId":"$attemptId","leaseToken":"$token","generation":$generation,"idempotencyKey":"${UUID.randomUUID()}","step":"PREPARE","evidenceSha256":"${"c".repeat(64)}"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.completedSteps") { value(1) }
        }

        mockMvc.post("/automation/runner/work-items/claims") {
            with(authentication(owner("write:automation")))
            contentType = MediaType.APPLICATION_JSON
            content = """{"workerId":"owner","generation":$generation}"""
        }.andExpect { status { isForbidden() } }
    }

    private fun owner(vararg scopes: String) = jwt("http://localhost/dev", "local-dev-user", *scopes)

    private fun runner() = jwt("http://localhost/runner", "runner", "report:automation-health")

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
}
