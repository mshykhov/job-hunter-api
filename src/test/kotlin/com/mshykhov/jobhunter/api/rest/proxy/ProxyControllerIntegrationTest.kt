package com.mshykhov.jobhunter.api.rest.proxy

import com.mshykhov.jobhunter.application.job.JobSource
import com.mshykhov.jobhunter.support.AbstractIntegrationTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

class ProxyControllerIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Nested
    inner class GetProxy {
        @Test
        fun `should return 503 when proxy service is disabled`() {
            mockMvc
                .get("/proxies") {
                    param("source", JobSource.DOU.value)
                }.andExpect {
                    status { isEqualTo(503) }
                }
        }

        @Test
        fun `should return 400 when source parameter is missing`() {
            mockMvc.get("/proxies").andExpect {
                status { isBadRequest() }
            }
        }

        @Test
        fun `should return 400 for invalid source value`() {
            mockMvc
                .get("/proxies") {
                    param("source", "INVALID")
                }.andExpect {
                    status { isBadRequest() }
                }
        }
    }

    @Nested
    inner class GetAllProxies {
        @Test
        fun `should return 503 when proxy service is disabled`() {
            mockMvc.get("/proxies/all").andExpect {
                status { isEqualTo(503) }
            }
        }
    }
}
