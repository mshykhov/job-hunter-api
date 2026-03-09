package com.mshykhov.jobhunter.api.rest.job

import com.fasterxml.jackson.databind.ObjectMapper
import com.mshykhov.jobhunter.application.job.JobSource
import com.mshykhov.jobhunter.application.job.JobSource.DJINNI
import com.mshykhov.jobhunter.application.job.JobSource.DOU
import com.mshykhov.jobhunter.application.userjob.UserJobStatus.APPLIED
import com.mshykhov.jobhunter.support.AbstractIntegrationTest
import com.mshykhov.jobhunter.support.TestFixtures
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import java.util.UUID

class JobControllerIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Nested
    inner class Ingest {
        @Test
        fun `should ingest new jobs and return them with generated ids`() {
            val requests =
                listOf(
                    TestFixtures.jobIngestRequest(
                        title = "Kotlin Developer",
                        url = "https://example.com/job-1",
                        source = DOU,
                    ),
                    TestFixtures.jobIngestRequest(
                        title = "Java Developer",
                        url = "https://example.com/job-2",
                        source = DJINNI,
                    ),
                )

            mockMvc
                .post("/jobs/ingest") {
                    contentType = APPLICATION_JSON
                    content = objectMapper.writeValueAsString(requests)
                }.andExpect {
                    status { isOk() }
                    jsonPath("$", hasSize<Any>(2))
                    jsonPath("$[0].id", notNullValue())
                    jsonPath("$[0].title", equalTo("Kotlin Developer"))
                    jsonPath("$[0].source", equalTo(DOU.value))
                    jsonPath("$[1].title", equalTo("Java Developer"))
                    jsonPath("$[1].source", equalTo(DJINNI.value))
                }
        }

        @Test
        fun `should deduplicate jobs by url on ingest`() {
            val url = "https://example.com/dedup-test"
            val requests =
                listOf(
                    TestFixtures.jobIngestRequest(title = "First Title", url = url),
                    TestFixtures.jobIngestRequest(title = "Second Title", url = url),
                )

            mockMvc
                .post("/jobs/ingest") {
                    contentType = APPLICATION_JSON
                    content = objectMapper.writeValueAsString(requests)
                }.andExpect {
                    status { isOk() }
                    jsonPath("$", hasSize<Any>(1))
                    jsonPath("$[0].title", equalTo("Second Title"))
                }
        }

        @Test
        fun `should update existing job when fields change`() {
            val url = "https://example.com/update-test"
            val original = listOf(TestFixtures.jobIngestRequest(title = "Original Title", url = url, salary = "3000 USD"))

            mockMvc
                .post("/jobs/ingest") {
                    contentType = APPLICATION_JSON
                    content = objectMapper.writeValueAsString(original)
                }.andExpect {
                    status { isOk() }
                    jsonPath("$[0].title", equalTo("Original Title"))
                    jsonPath("$[0].salary", equalTo("3000 USD"))
                }

            val updated = listOf(TestFixtures.jobIngestRequest(title = "Updated Title", url = url, salary = "5000 USD"))

            mockMvc
                .post("/jobs/ingest") {
                    contentType = APPLICATION_JSON
                    content = objectMapper.writeValueAsString(updated)
                }.andExpect {
                    status { isOk() }
                    jsonPath("$[0].title", equalTo("Updated Title"))
                    jsonPath("$[0].salary", equalTo("5000 USD"))
                }
        }

        @Test
        fun `should return unchanged job when no fields differ`() {
            val url = "https://example.com/unchanged-test"
            val request =
                TestFixtures.jobIngestRequest(
                    title = "Same Title",
                    url = url,
                    description = "Same desc",
                    salary = "4000 USD",
                )

            mockMvc
                .post("/jobs/ingest") {
                    contentType = APPLICATION_JSON
                    content = objectMapper.writeValueAsString(listOf(request))
                }.andExpect { status { isOk() } }

            mockMvc
                .post("/jobs/ingest") {
                    contentType = APPLICATION_JSON
                    content = objectMapper.writeValueAsString(listOf(request))
                }.andExpect {
                    status { isOk() }
                    jsonPath("$", hasSize<Any>(1))
                    jsonPath("$[0].title", equalTo("Same Title"))
                }
        }
    }

    @Nested
    inner class CheckJobs {
        @Test
        fun `should classify urls as new, updated, or unchanged`() {
            val existingUrl = "https://example.com/check-existing"
            val ingestRequest = TestFixtures.jobIngestRequest(title = "Existing Job", url = existingUrl, salary = "3000 USD")
            mockMvc
                .post("/jobs/ingest") {
                    contentType = APPLICATION_JSON
                    content = objectMapper.writeValueAsString(listOf(ingestRequest))
                }.andExpect { status { isOk() } }

            val checkRequests =
                listOf(
                    mapOf("url" to existingUrl, "title" to "Existing Job", "salary" to "3000 USD"),
                    mapOf("url" to existingUrl.replace("existing", "changed"), "title" to "Changed", "salary" to "5000 USD"),
                    mapOf("url" to "https://example.com/check-new"),
                )

            mockMvc
                .post("/jobs/check") {
                    contentType = APPLICATION_JSON
                    content = objectMapper.writeValueAsString(checkRequests)
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.newUrls", hasSize<Any>(2))
                    jsonPath("$.unchangedUrls", hasSize<Any>(1))
                    jsonPath("$.unchangedUrls[0]", equalTo(existingUrl))
                }
        }

        @Test
        fun `should return empty lists for empty request`() {
            mockMvc
                .post("/jobs/check") {
                    contentType = APPLICATION_JSON
                    content = "[]"
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.newUrls", hasSize<Any>(0))
                    jsonPath("$.updatedUrls", hasSize<Any>(0))
                    jsonPath("$.unchangedUrls", hasSize<Any>(0))
                }
        }
    }

    @Nested
    inner class PublicSearch {
        @Test
        fun `should search public jobs with pagination`() {
            val requests =
                (1..5).map { i ->
                    TestFixtures.jobIngestRequest(
                        title = "Public Search Job $i",
                        url = "https://example.com/public-search-$i",
                        source = DOU,
                        remote = true,
                    )
                }

            mockMvc
                .post("/jobs/ingest") {
                    contentType = APPLICATION_JSON
                    content = objectMapper.writeValueAsString(requests)
                }.andExpect { status { isOk() } }

            mockMvc
                .get("/public/jobs") {
                    param("page", "0")
                    param("size", "3")
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.content", hasSize<Any>(3))
                    jsonPath("$.page", equalTo(0))
                    jsonPath("$.size", equalTo(3))
                    jsonPath("$.totalPages", notNullValue())
                }
        }

        @Test
        fun `should filter public jobs by source`() {
            val douJob =
                TestFixtures.jobIngestRequest(
                    title = "DOU Filter Test",
                    url = "https://example.com/filter-dou-${System.nanoTime()}",
                    source = DOU,
                )
            val djinniJob =
                TestFixtures.jobIngestRequest(
                    title = "Djinni Filter Test",
                    url = "https://example.com/filter-djinni-${System.nanoTime()}",
                    source = DJINNI,
                )

            mockMvc
                .post("/jobs/ingest") {
                    contentType = APPLICATION_JSON
                    content = objectMapper.writeValueAsString(listOf(douJob, djinniJob))
                }.andExpect { status { isOk() } }

            mockMvc
                .get("/public/jobs") {
                    param("sources", "DJINNI")
                    param("search", "Djinni Filter Test")
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.content[0].source", equalTo(DJINNI.value))
                }
        }

        @Test
        fun `should return sources list`() {
            mockMvc
                .get("/public/jobs/sources")
                .andExpect {
                    status { isOk() }
                    jsonPath("$", hasSize<Any>(JobSource.entries.size))
                }
        }
    }

    @Nested
    inner class ErrorHandling {
        @Test
        fun `should return 404 when getting detail for non-existent job`() {
            mockMvc
                .get("/jobs/${UUID.randomUUID()}")
                .andExpect {
                    status { isNotFound() }
                    jsonPath("$.code", equalTo("NOT_FOUND"))
                }
        }

        @Test
        fun `should return 404 when updating status of non-existent job`() {
            mockMvc
                .patch("/jobs/${UUID.randomUUID()}/status") {
                    contentType = APPLICATION_JSON
                    content = """{"status": "${APPLIED.value}"}"""
                }.andExpect {
                    status { isNotFound() }
                    jsonPath("$.code", equalTo("NOT_FOUND"))
                }
        }

        @Test
        fun `should return 400 for bulk update with empty job ids`() {
            mockMvc
                .patch("/jobs/status") {
                    contentType = APPLICATION_JSON
                    content = """{"jobIds": [], "status": "${APPLIED.value}"}"""
                }.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.code", equalTo("VALIDATION_ERROR"))
                }
        }

        @Test
        fun `should return 400 for invalid JSON body`() {
            mockMvc
                .post("/jobs/ingest") {
                    contentType = APPLICATION_JSON
                    content = "not json"
                }.andExpect {
                    status { isBadRequest() }
                }
        }

        @Test
        fun `should return 400 for ingest with blank title`() {
            mockMvc
                .post("/jobs/ingest") {
                    contentType = APPLICATION_JSON
                    content =
                        """[{"title": "", "url": "https://example.com/blank-title", "source": "${DOU.value}"}]"""
                }.andExpect {
                    status { isBadRequest() }
                }
        }
    }
}
