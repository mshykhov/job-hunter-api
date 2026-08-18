package com.mshykhov.jobhunter.api.rest.automation

import com.mshykhov.jobhunter.support.AbstractIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put
import java.time.Instant

class AutomationControllerIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `owner can enable inspect and revoke delegation`() {
        mockMvc.put("/automation/delegation") { with(authentication(owner("write:automation"))) }.andExpect {
            status { isOk() }
            jsonPath("$.enabled") { value(true) }
        }
        mockMvc.get("/automation/status") { with(authentication(owner("read:automation"))) }.andExpect {
            status { isOk() }
            jsonPath("$.enabled") { value(true) }
        }
        mockMvc.delete("/automation/delegation") { with(authentication(owner("write:automation"))) }.andExpect {
            status { isNoContent() }
        }
    }

    @Test
    fun `non-owner and missing owner scope are forbidden`() {
        mockMvc.get("/automation/status") {
            with(authentication(jwt(OWNER_ISSUER, "another-user", "read:automation")))
        }.andExpect { status { isForbidden() } }

        mockMvc.get("/automation/status") { with(authentication(owner())) }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `runner token cannot use owner endpoint`() {
        mockMvc.get("/automation/status") { with(authentication(runner())) }.andExpect {
            status { isForbidden() }
        }
    }

    private fun owner(vararg scopes: String): JwtAuthenticationToken = jwt(OWNER_ISSUER, OWNER_SUBJECT, *scopes)

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
