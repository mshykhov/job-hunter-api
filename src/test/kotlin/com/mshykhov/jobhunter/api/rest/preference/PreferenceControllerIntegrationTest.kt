package com.mshykhov.jobhunter.api.rest.preference

import com.fasterxml.jackson.databind.ObjectMapper
import com.mshykhov.jobhunter.application.job.JobSource
import com.mshykhov.jobhunter.support.AbstractIntegrationTest
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

class PreferenceControllerIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Nested
    inner class SaveAbout {
        @Test
        fun `should save about text and return it`() {
            mockMvc
                .put("/preferences/about") {
                    contentType = APPLICATION_JSON
                    content = """{"about": "Experienced Kotlin developer"}"""
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.about", equalTo("Experienced Kotlin developer"))
                }
        }

        @Test
        fun `should overwrite previous about text`() {
            mockMvc
                .put("/preferences/about") {
                    contentType = APPLICATION_JSON
                    content = """{"about": "First version"}"""
                }.andExpect { status { isOk() } }

            mockMvc
                .put("/preferences/about") {
                    contentType = APPLICATION_JSON
                    content = """{"about": "Updated version"}"""
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.about", equalTo("Updated version"))
                }
        }

        @Test
        fun `should return 400 when about is blank`() {
            mockMvc
                .put("/preferences/about") {
                    contentType = APPLICATION_JSON
                    content = """{"about": ""}"""
                }.andExpect {
                    status { isBadRequest() }
                }
        }
    }

    @Nested
    inner class SaveSearch {
        @Test
        fun `should save search preferences`() {
            val body =
                mapOf(
                    "categories" to listOf("Backend", "DevOps"),
                    "locations" to listOf("Kyiv", "Remote"),
                    "remoteOnly" to true,
                    "disabledSources" to listOf(JobSource.LINKEDIN.value),
                )

            mockMvc
                .put("/preferences/search") {
                    contentType = APPLICATION_JSON
                    content = objectMapper.writeValueAsString(body)
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.categories", hasSize<Any>(2))
                    jsonPath("$.locations", hasSize<Any>(2))
                    jsonPath("$.remoteOnly", equalTo(true))
                    jsonPath("$.disabledSources", hasSize<Any>(1))
                }
        }

        @Test
        fun `should overwrite previous search preferences`() {
            mockMvc
                .put("/preferences/search") {
                    contentType = APPLICATION_JSON
                    content = """{"categories": ["Backend"], "locations": ["Kyiv"], "remoteOnly": true, "disabledSources": []}"""
                }.andExpect { status { isOk() } }

            mockMvc
                .put("/preferences/search") {
                    contentType = APPLICATION_JSON
                    content = """{"categories": ["Frontend"], "locations": [], "remoteOnly": false, "disabledSources": []}"""
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.categories[0]", equalTo("Frontend"))
                    jsonPath("$.locations", hasSize<Any>(0))
                    jsonPath("$.remoteOnly", equalTo(false))
                }
        }

        @Test
        fun `should accept empty lists as valid input`() {
            mockMvc
                .put("/preferences/search") {
                    contentType = APPLICATION_JSON
                    content = """{"categories": [], "locations": [], "remoteOnly": false, "disabledSources": []}"""
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.categories", hasSize<Any>(0))
                }
        }
    }

    @Nested
    inner class SaveMatching {
        @Test
        fun `should save matching preferences`() {
            val body =
                mapOf(
                    "excludedKeywords" to listOf("PHP"),
                    "excludedTitleKeywords" to listOf("Junior"),
                    "excludedCompanies" to listOf("BadCorp"),
                    "matchWithAi" to true,
                )

            mockMvc
                .put("/preferences/matching") {
                    contentType = APPLICATION_JSON
                    content = objectMapper.writeValueAsString(body)
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.excludedKeywords", hasSize<Any>(1))
                    jsonPath("$.excludedTitleKeywords", hasSize<Any>(1))
                    jsonPath("$.excludedCompanies", hasSize<Any>(1))
                    jsonPath("$.matchWithAi", equalTo(true))
                }
        }

        @Test
        fun `should save custom prompt`() {
            val body =
                mapOf(
                    "customPrompt" to "Focus on Kotlin and remote positions",
                )

            mockMvc
                .put("/preferences/matching") {
                    contentType = APPLICATION_JSON
                    content = objectMapper.writeValueAsString(body)
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.customPrompt", equalTo("Focus on Kotlin and remote positions"))
                }
        }
    }

    @Nested
    inner class SaveTelegram {
        @Test
        fun `should save telegram preferences`() {
            val body =
                mapOf(
                    "chatId" to "123456",
                    "username" to "testuser",
                    "notificationsEnabled" to true,
                    "notificationSources" to listOf("dou", "djinni"),
                )

            mockMvc
                .put("/preferences/telegram") {
                    contentType = APPLICATION_JSON
                    content = objectMapper.writeValueAsString(body)
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.chatId", equalTo("123456"))
                    jsonPath("$.username", equalTo("testuser"))
                    jsonPath("$.notificationsEnabled", equalTo(true))
                }
        }

        @Test
        fun `should accept null chatId and username`() {
            val body =
                mapOf(
                    "chatId" to null,
                    "username" to null,
                    "notificationsEnabled" to false,
                    "notificationSources" to emptyList<String>(),
                )

            mockMvc
                .put("/preferences/telegram") {
                    contentType = APPLICATION_JSON
                    content = objectMapper.writeValueAsString(body)
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.notificationsEnabled", equalTo(false))
                }
        }
    }

    @Nested
    inner class GetPreferences {
        @Test
        fun `should return saved preferences after updates`() {
            mockMvc
                .put("/preferences/about") {
                    contentType = APPLICATION_JSON
                    content = """{"about": "My about text"}"""
                }.andExpect { status { isOk() } }

            mockMvc
                .put("/preferences/search") {
                    contentType = APPLICATION_JSON
                    content = """{"categories": ["Backend"], "locations": [], "remoteOnly": true, "disabledSources": []}"""
                }.andExpect { status { isOk() } }

            mockMvc.get("/preferences").andExpect {
                status { isOk() }
                jsonPath("$.about", equalTo("My about text"))
                jsonPath("$.search.categories[0]", equalTo("Backend"))
                jsonPath("$.search.remoteOnly", equalTo(true))
            }
        }
    }

    @Nested
    inner class GeneratePreferences {
        @Test
        fun `should return 422 when user has no AI settings`() {
            mockMvc
                .put("/preferences/about") {
                    contentType = APPLICATION_JSON
                    content = """{"about": "Some about text"}"""
                }.andExpect { status { isOk() } }

            mockMvc.post("/preferences/generate").andExpect {
                status { isEqualTo(422) }
            }
        }
    }
}
