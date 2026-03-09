package com.mshykhov.jobhunter.application.userjob

import com.mshykhov.jobhunter.api.rest.job.dto.UserJobFilterRequest
import com.mshykhov.jobhunter.application.common.NotFoundException
import com.mshykhov.jobhunter.application.job.JobSource
import com.mshykhov.jobhunter.application.user.UserFacade
import com.mshykhov.jobhunter.support.TestFixtures
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import java.time.Instant
import java.util.UUID

class UserJobServiceTest {
    private val userFacade = mockk<UserFacade>()
    private val userJobFacade = mockk<UserJobFacade>()
    private val service = UserJobService(userFacade, userJobFacade)

    private val auth0Sub = "auth0|test-user"
    private val user = TestFixtures.userEntity(auth0Sub = auth0Sub)
    private val job = TestFixtures.jobEntity()
    private val userJob = TestFixtures.userJobEntity(user = user, job = job)

    @Nested
    inner class Search {
        @Test
        fun `should return empty response when user does not exist`() {
            every { userFacade.findByAuth0Sub(auth0Sub) } returns null

            val result = service.search(auth0Sub, UserJobFilterRequest())

            assertEquals(0, result.totalElements)
            assertTrue(result.content.isEmpty())
            assertTrue(result.statusCounts.isEmpty())
        }

        @Test
        fun `should return paginated results for existing user`() {
            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userJobFacade.findAll(any<Specification<UserJobEntity>>(), any<PageRequest>()) } returns
                PageImpl(listOf(userJob))
            every { userJobFacade.count(any()) } returns 1L

            val result = service.search(auth0Sub, UserJobFilterRequest())

            assertEquals(1, result.totalElements)
            assertEquals(1, result.content.size)
        }

        @Test
        fun `should include statusCounts for all UserJobStatus values`() {
            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userJobFacade.findAll(any<Specification<UserJobEntity>>(), any<PageRequest>()) } returns
                PageImpl(emptyList())
            every { userJobFacade.count(any()) } returns 0L

            val result = service.search(auth0Sub, UserJobFilterRequest())

            assertEquals(UserJobStatus.entries.size, result.statusCounts.size)
            UserJobStatus.entries.forEach { status ->
                assertTrue(result.statusCounts.containsKey(status))
            }
        }

        @Test
        fun `should clamp page size to max 100`() {
            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userJobFacade.findAll(any<Specification<UserJobEntity>>(), any<PageRequest>()) } answers {
                val pageable = secondArg<PageRequest>()
                PageImpl(emptyList(), pageable, 0)
            }
            every { userJobFacade.count(any()) } returns 0L

            val result = service.search(auth0Sub, UserJobFilterRequest(size = 500))

            assertEquals(100, result.size)
        }

        @Test
        fun `should apply status filter`() {
            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userJobFacade.findAll(any<Specification<UserJobEntity>>(), any<PageRequest>()) } returns
                PageImpl(emptyList())
            every { userJobFacade.count(any()) } returns 0L

            service.search(auth0Sub, UserJobFilterRequest(statuses = listOf(UserJobStatus.NEW)))

            verify { userJobFacade.findAll(any<Specification<UserJobEntity>>(), any<PageRequest>()) }
        }

        @Test
        fun `should apply source filter`() {
            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userJobFacade.findAll(any<Specification<UserJobEntity>>(), any<PageRequest>()) } returns
                PageImpl(emptyList())
            every { userJobFacade.count(any()) } returns 0L

            service.search(auth0Sub, UserJobFilterRequest(sources = listOf(JobSource.DOU)))

            verify { userJobFacade.findAll(any<Specification<UserJobEntity>>(), any<PageRequest>()) }
        }

        @Test
        fun `should apply publishedAfter filter`() {
            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userJobFacade.findAll(any<Specification<UserJobEntity>>(), any<PageRequest>()) } returns
                PageImpl(emptyList())
            every { userJobFacade.count(any()) } returns 0L

            service.search(auth0Sub, UserJobFilterRequest(publishedAfter = Instant.now()))

            verify { userJobFacade.findAll(any<Specification<UserJobEntity>>(), any<PageRequest>()) }
        }

        @Test
        fun `should apply remote filter`() {
            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userJobFacade.findAll(any<Specification<UserJobEntity>>(), any<PageRequest>()) } returns
                PageImpl(emptyList())
            every { userJobFacade.count(any()) } returns 0L

            service.search(auth0Sub, UserJobFilterRequest(remote = true))

            verify { userJobFacade.findAll(any<Specification<UserJobEntity>>(), any<PageRequest>()) }
        }

        @Test
        fun `should apply search text filter`() {
            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userJobFacade.findAll(any<Specification<UserJobEntity>>(), any<PageRequest>()) } returns
                PageImpl(emptyList())
            every { userJobFacade.count(any()) } returns 0L

            service.search(auth0Sub, UserJobFilterRequest(search = "kotlin"))

            verify { userJobFacade.findAll(any<Specification<UserJobEntity>>(), any<PageRequest>()) }
        }

        @Test
        fun `should apply minScore filter`() {
            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userJobFacade.findAll(any<Specification<UserJobEntity>>(), any<PageRequest>()) } returns
                PageImpl(emptyList())
            every { userJobFacade.count(any()) } returns 0L

            service.search(auth0Sub, UserJobFilterRequest(minScore = 50))

            verify { userJobFacade.findAll(any<Specification<UserJobEntity>>(), any<PageRequest>()) }
        }
    }

    @Nested
    inner class GetDetail {
        private val jobId = UUID.randomUUID()

        @Test
        fun `should return user job entity when found`() {
            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userJobFacade.findByUserIdAndJobId(user.id, jobId) } returns userJob

            val result = service.getDetail(auth0Sub, jobId)

            assertEquals(userJob, result)
        }

        @Test
        fun `should throw NotFoundException when user does not exist`() {
            every { userFacade.findByAuth0Sub(auth0Sub) } returns null

            assertThrows<NotFoundException> {
                service.getDetail(auth0Sub, jobId)
            }
        }

        @Test
        fun `should throw NotFoundException when job not in user list`() {
            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userJobFacade.findByUserIdAndJobId(user.id, jobId) } returns null

            assertThrows<NotFoundException> {
                service.getDetail(auth0Sub, jobId)
            }
        }
    }

    @Nested
    inner class UpdateStatus {
        private val jobId = UUID.randomUUID()

        @Test
        fun `should update status and save`() {
            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userJobFacade.findByUserIdAndJobId(user.id, jobId) } returns userJob
            every { userJobFacade.save(userJob) } returns userJob

            val result = service.updateStatus(auth0Sub, jobId, UserJobStatus.APPLIED)

            assertEquals(UserJobStatus.APPLIED, result.status)
            verify { userJobFacade.save(userJob) }
        }

        @Test
        fun `should throw NotFoundException when user does not exist`() {
            every { userFacade.findByAuth0Sub(auth0Sub) } returns null

            assertThrows<NotFoundException> {
                service.updateStatus(auth0Sub, jobId, UserJobStatus.APPLIED)
            }
        }

        @Test
        fun `should throw NotFoundException when job not in user list`() {
            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userJobFacade.findByUserIdAndJobId(user.id, jobId) } returns null

            assertThrows<NotFoundException> {
                service.updateStatus(auth0Sub, jobId, UserJobStatus.APPLIED)
            }
        }
    }

    @Nested
    inner class BulkUpdateStatus {
        @Test
        fun `should update status for all provided job IDs`() {
            val job2 = TestFixtures.jobEntity()
            val userJob2 = TestFixtures.userJobEntity(user = user, job = job2)
            val jobIds = listOf(job.id, job2.id)

            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userJobFacade.findByUserIdAndJobIds(user.id, jobIds) } returns listOf(userJob, userJob2)
            every { userJobFacade.saveAll(any<List<UserJobEntity>>()) } answers { firstArg() }

            val result = service.bulkUpdateStatus(auth0Sub, jobIds, UserJobStatus.IRRELEVANT)

            assertEquals(2, result.size)
            result.forEach { assertEquals(UserJobStatus.IRRELEVANT, it.status) }
        }

        @Test
        fun `should silently skip job IDs not belonging to user`() {
            val missingJobId = UUID.randomUUID()
            val jobIds = listOf(job.id, missingJobId)

            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userJobFacade.findByUserIdAndJobIds(user.id, jobIds) } returns listOf(userJob)
            every { userJobFacade.saveAll(any<List<UserJobEntity>>()) } answers { firstArg() }

            val result = service.bulkUpdateStatus(auth0Sub, jobIds, UserJobStatus.APPLIED)

            assertEquals(1, result.size)
        }

        @Test
        fun `should throw NotFoundException when user does not exist`() {
            every { userFacade.findByAuth0Sub(auth0Sub) } returns null

            assertThrows<NotFoundException> {
                service.bulkUpdateStatus(auth0Sub, listOf(UUID.randomUUID()), UserJobStatus.APPLIED)
            }
        }

        @Test
        fun `should handle empty jobIds list`() {
            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userJobFacade.findByUserIdAndJobIds(user.id, emptyList()) } returns emptyList()
            every { userJobFacade.saveAll(emptyList()) } returns emptyList()

            val result = service.bulkUpdateStatus(auth0Sub, emptyList(), UserJobStatus.APPLIED)

            assertTrue(result.isEmpty())
        }
    }
}
