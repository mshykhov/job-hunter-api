package com.mshykhov.jobhunter.application.job

import com.mshykhov.jobhunter.api.rest.job.dto.JobCheckRequest
import com.mshykhov.jobhunter.support.TestFixtures
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import java.time.Instant

class JobServiceTest {
    private val jobFacade = mockk<JobFacade>()
    private val service = JobService(jobFacade)

    @Nested
    inner class Ingest {
        @Test
        fun `should create new job when URL does not exist`() {
            val request = TestFixtures.jobIngestRequest(url = "https://example.com/new-job")
            every { jobFacade.findByUrls(listOf(request.url)) } returns emptyList()
            every { jobFacade.saveAll(any()) } answers { firstArg() }

            val result = service.ingest(listOf(request))

            assertEquals(1, result.size)
            assertEquals(request.title, result[0].title)
            assertEquals(request.url, result[0].url)
        }

        @Test
        fun `should update existing job when fields changed`() {
            val url = "https://example.com/existing"
            val existing = TestFixtures.jobEntity(url = url, title = "Old Title", salary = "3000 USD")
            val request = TestFixtures.jobIngestRequest(url = url, title = "New Title", salary = "5000 USD")

            every { jobFacade.findByUrls(listOf(url)) } returns listOf(existing)
            every { jobFacade.saveAll(any()) } answers { firstArg() }

            val result = service.ingest(listOf(request))

            assertEquals(1, result.size)
            assertEquals("New Title", existing.title)
            assertEquals("5000 USD", existing.salary)
        }

        @Test
        fun `should not update when all fields are identical`() {
            val url = "https://example.com/same"
            val existing =
                TestFixtures.jobEntity(
                    url = url,
                    title = "Same Title",
                    description = "Same description",
                    salary = "5000 USD",
                    location = "Remote",
                    remote = true,
                )
            val request =
                TestFixtures.jobIngestRequest(
                    url = url,
                    title = "Same Title",
                    description = "Same description",
                    salary = "5000 USD",
                    location = "Remote",
                    remote = true,
                    publishedAt = null,
                )

            every { jobFacade.findByUrls(listOf(url)) } returns listOf(existing)
            val savedSlot = slot<List<JobEntity>>()
            every { jobFacade.saveAll(capture(savedSlot)) } answers { firstArg() }

            service.ingest(listOf(request))

            assertTrue(savedSlot.captured.isEmpty())
        }

        @Test
        fun `should deduplicate requests by URL keeping last`() {
            val url = "https://example.com/dup"
            val request1 = TestFixtures.jobIngestRequest(url = url, title = "First")
            val request2 = TestFixtures.jobIngestRequest(url = url, title = "Second")

            every { jobFacade.findByUrls(listOf(url)) } returns emptyList()
            every { jobFacade.saveAll(any()) } answers { firstArg() }

            val result = service.ingest(listOf(request1, request2))

            assertEquals(1, result.size)
            assertEquals("Second", result[0].title)
        }

        @Test
        fun `should parse publishedAt in ISO-8601 format`() {
            val request = TestFixtures.jobIngestRequest(publishedAt = "2026-03-01T10:00:00Z")
            every { jobFacade.findByUrls(any()) } returns emptyList()
            every { jobFacade.saveAll(any()) } answers { firstArg() }

            val result = service.ingest(listOf(request))

            assertEquals(Instant.parse("2026-03-01T10:00:00Z"), result[0].publishedAt)
        }

        @Test
        fun `should keep existing publishedAt when new value is unparseable`() {
            val url = "https://example.com/keep-date"
            val existingDate = Instant.parse("2026-01-01T00:00:00Z")
            val existing = TestFixtures.jobEntity(url = url, title = "Old").apply { publishedAt = existingDate }
            val request = TestFixtures.jobIngestRequest(url = url, title = "Updated", publishedAt = "invalid-date")

            every { jobFacade.findByUrls(listOf(url)) } returns listOf(existing)
            every { jobFacade.saveAll(any()) } answers { firstArg() }

            service.ingest(listOf(request))

            assertEquals(existingDate, existing.publishedAt)
        }

        @Test
        fun `should handle empty request list`() {
            every { jobFacade.findByUrls(emptyList()) } returns emptyList()
            every { jobFacade.saveAll(emptyList()) } returns emptyList()

            val result = service.ingest(emptyList())

            assertTrue(result.isEmpty())
        }

        @Test
        fun `should handle mix of new, updated, and unchanged jobs`() {
            val newUrl = "https://example.com/new"
            val updatedUrl = "https://example.com/updated"
            val unchangedUrl = "https://example.com/unchanged"

            val existingUpdated = TestFixtures.jobEntity(url = updatedUrl, title = "Old Title")
            val existingUnchanged =
                TestFixtures.jobEntity(
                    url = unchangedUrl,
                    title = "Same",
                    description = "Same desc",
                    salary = null,
                    location = null,
                    remote = null,
                )

            every { jobFacade.findByUrls(any()) } returns listOf(existingUpdated, existingUnchanged)
            every { jobFacade.saveAll(any()) } answers { firstArg() }

            val requests =
                listOf(
                    TestFixtures.jobIngestRequest(url = newUrl, title = "New Job"),
                    TestFixtures.jobIngestRequest(url = updatedUrl, title = "New Title"),
                    TestFixtures.jobIngestRequest(
                        url = unchangedUrl,
                        title = "Same",
                        description = "Same desc",
                        salary = null,
                        location = null,
                        remote = null,
                        publishedAt = null,
                    ),
                )

            val result = service.ingest(requests)

            assertEquals(3, result.size)
        }

        @Test
        fun `should save only changed entities`() {
            val unchangedUrl = "https://example.com/no-change"
            val existing =
                TestFixtures.jobEntity(
                    url = unchangedUrl,
                    title = "Title",
                    description = "Desc",
                    salary = null,
                    location = null,
                    remote = null,
                )

            every { jobFacade.findByUrls(any()) } returns listOf(existing)
            val savedSlot = slot<List<JobEntity>>()
            every { jobFacade.saveAll(capture(savedSlot)) } answers { firstArg() }

            service.ingest(
                listOf(
                    TestFixtures.jobIngestRequest(
                        url = unchangedUrl,
                        title = "Title",
                        description = "Desc",
                        salary = null,
                        location = null,
                        remote = null,
                        publishedAt = null,
                    ),
                ),
            )

            assertTrue(savedSlot.captured.isEmpty())
        }
    }

    @Nested
    inner class CheckJobs {
        @Test
        fun `should classify brand new URL as new`() {
            val request = JobCheckRequest(url = "https://example.com/brand-new")
            every { jobFacade.findByUrls(listOf(request.url)) } returns emptyList()

            val result = service.checkJobs(listOf(request))

            assertEquals(listOf(request.url), result.newUrls)
            assertTrue(result.updatedUrls.isEmpty())
            assertTrue(result.unchangedUrls.isEmpty())
        }

        @Test
        fun `should classify existing URL with no changes as unchanged`() {
            val url = "https://example.com/same"
            val existing = TestFixtures.jobEntity(url = url, title = "Same Title", salary = "5000")
            val request = JobCheckRequest(url = url, title = "Same Title", salary = "5000")

            every { jobFacade.findByUrls(listOf(url)) } returns listOf(existing)

            val result = service.checkJobs(listOf(request))

            assertTrue(result.newUrls.isEmpty())
            assertTrue(result.updatedUrls.isEmpty())
            assertEquals(listOf(url), result.unchangedUrls)
        }

        @Test
        fun `should classify existing URL with title change as updated`() {
            val url = "https://example.com/changed"
            val existing = TestFixtures.jobEntity(url = url, title = "Old Title")
            val request = JobCheckRequest(url = url, title = "New Title")

            every { jobFacade.findByUrls(listOf(url)) } returns listOf(existing)

            val result = service.checkJobs(listOf(request))

            assertTrue(result.newUrls.isEmpty())
            assertEquals(listOf(url), result.updatedUrls)
        }

        @Test
        fun `should classify existing URL with salary change as updated`() {
            val url = "https://example.com/salary-changed"
            val existing = TestFixtures.jobEntity(url = url, salary = "3000 USD")
            val request = JobCheckRequest(url = url, salary = "5000 USD")

            every { jobFacade.findByUrls(listOf(url)) } returns listOf(existing)

            val result = service.checkJobs(listOf(request))

            assertEquals(listOf(url), result.updatedUrls)
        }

        @Test
        fun `should ignore null fields in check request`() {
            val url = "https://example.com/partial"
            val existing = TestFixtures.jobEntity(url = url, title = "Title", salary = "5000")
            val request = JobCheckRequest(url = url)

            every { jobFacade.findByUrls(listOf(url)) } returns listOf(existing)

            val result = service.checkJobs(listOf(request))

            assertEquals(listOf(url), result.unchangedUrls)
        }

        @Test
        fun `should return all-empty lists for empty input`() {
            val result = service.checkJobs(emptyList())

            assertTrue(result.newUrls.isEmpty())
            assertTrue(result.updatedUrls.isEmpty())
            assertTrue(result.unchangedUrls.isEmpty())
        }

        @Test
        fun `should deduplicate check requests by URL`() {
            val url = "https://example.com/dup"
            val request1 = JobCheckRequest(url = url, title = "First")
            val request2 = JobCheckRequest(url = url, title = "Second")

            every { jobFacade.findByUrls(listOf(url)) } returns emptyList()

            val result = service.checkJobs(listOf(request1, request2))

            assertEquals(1, result.newUrls.size)
        }
    }

    @Nested
    inner class SearchPublic {
        @Test
        fun `should return paginated results`() {
            every { jobFacade.findAll(any<Specification<JobEntity>>(), any<PageRequest>()) } answers {
                val pageable = secondArg<PageRequest>()
                PageImpl(emptyList(), pageable, 0)
            }

            val result = service.searchPublic(page = 0, size = 10, search = null, sources = null, remote = null, publishedAfter = null)

            assertEquals(0, result.page)
            assertEquals(10, result.size)
        }

        @Test
        fun `should clamp page size to max 100`() {
            every { jobFacade.findAll(any<Specification<JobEntity>>(), any<PageRequest>()) } answers {
                val pageable = secondArg<PageRequest>()
                PageImpl(emptyList(), pageable, 0)
            }

            val result = service.searchPublic(page = 0, size = 500, search = null, sources = null, remote = null, publishedAfter = null)

            assertEquals(100, result.size)
        }

        @Test
        fun `should clamp page to minimum 0`() {
            every { jobFacade.findAll(any<Specification<JobEntity>>(), any<PageRequest>()) } answers {
                val pageable = secondArg<PageRequest>()
                PageImpl(emptyList(), pageable, 0)
            }

            val result = service.searchPublic(page = -5, size = 10, search = null, sources = null, remote = null, publishedAfter = null)

            assertEquals(0, result.page)
        }

        @Test
        fun `should apply search filter`() {
            every { jobFacade.findAll(any<Specification<JobEntity>>(), any<PageRequest>()) } answers {
                PageImpl(emptyList(), secondArg<PageRequest>(), 0)
            }

            service.searchPublic(page = 0, size = 10, search = "kotlin", sources = null, remote = null, publishedAfter = null)

            verify { jobFacade.findAll(any<Specification<JobEntity>>(), any<PageRequest>()) }
        }

        @Test
        fun `should apply sources filter`() {
            every { jobFacade.findAll(any<Specification<JobEntity>>(), any<PageRequest>()) } answers {
                PageImpl(emptyList(), secondArg<PageRequest>(), 0)
            }

            service.searchPublic(
                page = 0,
                size = 10,
                search = null,
                sources = listOf(JobSource.DOU, JobSource.DJINNI),
                remote = null,
                publishedAfter = null,
            )

            verify { jobFacade.findAll(any<Specification<JobEntity>>(), any<PageRequest>()) }
        }

        @Test
        fun `should apply remote filter`() {
            every { jobFacade.findAll(any<Specification<JobEntity>>(), any<PageRequest>()) } answers {
                PageImpl(emptyList(), secondArg<PageRequest>(), 0)
            }

            service.searchPublic(page = 0, size = 10, search = null, sources = null, remote = true, publishedAfter = null)

            verify { jobFacade.findAll(any<Specification<JobEntity>>(), any<PageRequest>()) }
        }

        @Test
        fun `should apply publishedAfter filter`() {
            every { jobFacade.findAll(any<Specification<JobEntity>>(), any<PageRequest>()) } answers {
                PageImpl(emptyList(), secondArg<PageRequest>(), 0)
            }

            service.searchPublic(page = 0, size = 10, search = null, sources = null, remote = null, publishedAfter = Instant.now())

            verify { jobFacade.findAll(any<Specification<JobEntity>>(), any<PageRequest>()) }
        }
    }
}
