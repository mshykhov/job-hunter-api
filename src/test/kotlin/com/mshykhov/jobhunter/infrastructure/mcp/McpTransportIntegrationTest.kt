package com.mshykhov.jobhunter.infrastructure.mcp

import com.mshykhov.jobhunter.support.AbstractIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

class McpTransportIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `should initialize the stateless MCP transport`() {
        mockMvc
            .post("/mcp") {
                contentType = MediaType.APPLICATION_JSON
                header("Accept", "application/json, text/event-stream")
                content =
                    """
                    {
                      "jsonrpc": "2.0",
                      "id": 1,
                      "method": "initialize",
                      "params": {
                        "protocolVersion": "2025-03-26",
                        "capabilities": {},
                        "clientInfo": {
                          "name": "job-hunter-contract-test",
                          "version": "1.0.0"
                        }
                      }
                    }
                    """.trimIndent()
            }.andExpect {
                status { isOk() }
                jsonPath("$.result.serverInfo.name") { value("job-hunter") }
                jsonPath("$.result.protocolVersion") { isNotEmpty() }
            }
    }
}
