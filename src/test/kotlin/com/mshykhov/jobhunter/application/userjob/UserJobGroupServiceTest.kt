package com.mshykhov.jobhunter.application.userjob

import com.mshykhov.jobhunter.api.rest.job.dto.UserJobGroupFilterRequest
import com.mshykhov.jobhunter.application.common.NotFoundException
import com.mshykhov.jobhunter.application.job.JobFacade
import com.mshykhov.jobhunter.application.user.UserFacade
import com.mshykhov.jobhunter.support.TestFixtures
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserJobGroupServiceTest {
    private val userFacade = mockk<UserFacade>()
    private val userJobGroupFacade = mockk<UserJobGroupFacade>()
    private val userJobFacade = mockk<UserJobFacade>()
    private val jobFacade = mockk<JobFacade>()

    private val service = UserJobGroupService(userFacade, userJobGroupFacade, userJobFacade, jobFacade)

    private val auth0Sub = "auth0|test-user"
    private val user = TestFixtures.userEntity(auth0Sub = auth0Sub)

    @Nested
    inner class Search {
        @Test
        fun `should return paginated groups with status counts`() {
            val group = TestFixtures.jobGroupEntity()
            val userJobGroup = TestFixtures.userJobGroupEntity(user = user, group = group)
            val filter = UserJobGroupFilterRequest()

            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userJobGroupFacade.findAll(any<Specification<UserJobGroupEntity>>(), any<PageRequest>()) } returns
                PageImpl(listOf(userJobGroup), PageRequest.of(0, 50), 1)
            every { userJobGroupFacade.count(any()) } returns 0L

            val result = service.search(auth0Sub, filter)

            assertEquals(1, result.content.size)
            assertEquals(1, result.totalElements)
        }

        @Test
        fun `should return empty response when user does not exist`() {
            val filter = UserJobGroupFilterRequest()

            every { userFacade.findByAuth0Sub(auth0Sub) } returns null

            val result = service.search(auth0Sub, filter)

            assertTrue(result.content.isEmpty())
            assertEquals(0, result.totalElements)
            assertEquals(0, result.totalPages)
        }

        @Test
        fun `should clamp page size to max 100`() {
            val filter = UserJobGroupFilterRequest(size = 500)

            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userJobGroupFacade.findAll(any<Specification<UserJobGroupEntity>>(), any<PageRequest>()) } answers {
                val pageable = secondArg<PageRequest>()
                PageImpl(emptyList(), pageable, 0)
            }
            every { userJobGroupFacade.count(any()) } returns 0L

            val result = service.search(auth0Sub, filter)

            assertEquals(100, result.size)
        }

        @Test
        fun `should clamp page to minimum 0`() {
            val filter = UserJobGroupFilterRequest(page = -5)

            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userJobGroupFacade.findAll(any<Specification<UserJobGroupEntity>>(), any<PageRequest>()) } answers {
                val pageable = secondArg<PageRequest>()
                PageImpl(emptyList(), pageable, 0)
            }
            every { userJobGroupFacade.count(any()) } returns 0L

            val result = service.search(auth0Sub, filter)

            assertEquals(0, result.page)
        }

        @Test
        fun `should compute status counts for all statuses`() {
            val filter = UserJobGroupFilterRequest()

            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userJobGroupFacade.findAll(any<Specification<UserJobGroupEntity>>(), any<PageRequest>()) } returns
                PageImpl(emptyList(), PageRequest.of(0, 50), 0)
            every { userJobGroupFacade.count(any()) } returns 0L

            val result = service.search(auth0Sub, filter)

            assertEquals(UserJobStatus.entries.size, result.statusCounts.size)
            UserJobStatus.entries.forEach { status ->
                assertTrue(status in result.statusCounts)
            }
        }

        @Test
        fun `should use matched sort when specified`() {
            val filter = UserJobGroupFilterRequest(sortBy = UserJobGroupSort.MATCHED)

            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userJobGroupFacade.findAll(any<Specification<UserJobGroupEntity>>(), any<PageRequest>()) } answers {
                val pageable = secondArg<PageRequest>()
                assertEquals(UserJobGroupSort.MATCHED.sort, pageable.sort)
                PageImpl(emptyList(), pageable, 0)
            }
            every { userJobGroupFacade.count(any()) } returns 0L

            service.search(auth0Sub, filter)
        }

        @Test
        fun `should compute non-zero status counts`() {
            val filter = UserJobGroupFilterRequest()

            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userJobGroupFacade.findAll(any<Specification<UserJobGroupEntity>>(), any<PageRequest>()) } returns
                PageImpl(emptyList(), PageRequest.of(0, 50), 0)
            every { userJobGroupFacade.count(any()) } returnsMany listOf(5L, 2L, 1L)

            val result = service.search(auth0Sub, filter)

            assertEquals(5L, result.statusCounts[UserJobStatus.NEW])
            assertEquals(2L, result.statusCounts[UserJobStatus.APPLIED])
            assertEquals(1L, result.statusCounts[UserJobStatus.IRRELEVANT])
        }

        @Test
        fun `should use default sort by score`() {
            val filter = UserJobGroupFilterRequest()

            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userJobGroupFacade.findAll(any<Specification<UserJobGroupEntity>>(), any<PageRequest>()) } answers {
                val pageable = secondArg<PageRequest>()
                assertEquals(UserJobGroupSort.SCORE.sort, pageable.sort)
                PageImpl(emptyList(), pageable, 0)
            }
            every { userJobGroupFacade.count(any()) } returns 0L

            service.search(auth0Sub, filter)
        }
    }

    @Nested
    inner class GetGroupDetail {
        private val groupId = UUID.randomUUID()

        @Test
        fun `should return group detail with job variants`() {
            val group = TestFixtures.jobGroupEntity()
            val userJobGroup = TestFixtures.userJobGroupEntity(user = user, group = group)
            val job = TestFixtures.jobEntity(group = group)

            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userJobGroupFacade.findByUserIdAndGroupId(user.id, groupId) } returns userJobGroup
            every { jobFacade.findByGroupId(groupId) } returns listOf(job)
            every { userJobFacade.findByUserIdAndJobIds(user.id, listOf(job.id)) } returns emptyList()

            val result = service.getGroupDetail(auth0Sub, groupId)

            assertEquals(group.id, result.groupId)
            assertEquals(group.title, result.title)
            assertEquals(1, result.jobs.size)
        }

        @Test
        fun `should include user job outreach data for each job variant`() {
            val group = TestFixtures.jobGroupEntity()
            val userJobGroup = TestFixtures.userJobGroupEntity(user = user, group = group)
            val job = TestFixtures.jobEntity(group = group)
            val userJob = TestFixtures.userJobEntity(user = user, job = job).apply { coverLetter = "My cover letter" }

            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userJobGroupFacade.findByUserIdAndGroupId(user.id, groupId) } returns userJobGroup
            every { jobFacade.findByGroupId(groupId) } returns listOf(job)
            every { userJobFacade.findByUserIdAndJobIds(user.id, listOf(job.id)) } returns listOf(userJob)

            val result = service.getGroupDetail(auth0Sub, groupId)

            assertEquals("My cover letter", result.jobs[0].coverLetter)
        }

        @Test
        fun `should include recruiter message for each job variant`() {
            val group = TestFixtures.jobGroupEntity()
            val userJobGroup = TestFixtures.userJobGroupEntity(user = user, group = group)
            val job = TestFixtures.jobEntity(group = group)
            val userJob = TestFixtures.userJobEntity(user = user, job = job).apply { recruiterMessage = "Hello recruiter" }

            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userJobGroupFacade.findByUserIdAndGroupId(user.id, groupId) } returns userJobGroup
            every { jobFacade.findByGroupId(groupId) } returns listOf(job)
            every { userJobFacade.findByUserIdAndJobIds(user.id, listOf(job.id)) } returns listOf(userJob)

            val result = service.getGroupDetail(auth0Sub, groupId)

            assertEquals("Hello recruiter", result.jobs[0].recruiterMessage)
        }

        @Test
        fun `should handle group with no jobs`() {
            val group = TestFixtures.jobGroupEntity()
            val userJobGroup = TestFixtures.userJobGroupEntity(user = user, group = group)

            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userJobGroupFacade.findByUserIdAndGroupId(user.id, groupId) } returns userJobGroup
            every { jobFacade.findByGroupId(groupId) } returns emptyList()
            every { userJobFacade.findByUserIdAndJobIds(user.id, emptyList()) } returns emptyList()

            val result = service.getGroupDetail(auth0Sub, groupId)

            assertTrue(result.jobs.isEmpty())
        }

        @Nested
        inner class ErrorHandling {
            @Test
            fun `should throw NotFoundException when user does not exist`() {
                every { userFacade.findByAuth0Sub(auth0Sub) } returns null

                assertThrows<NotFoundException> {
                    service.getGroupDetail(auth0Sub, groupId)
                }
            }

            @Test
            fun `should throw NotFoundException when group not found for user`() {
                every { userFacade.findByAuth0Sub(auth0Sub) } returns user
                every { userJobGroupFacade.findByUserIdAndGroupId(user.id, groupId) } returns null

                assertThrows<NotFoundException> {
                    service.getGroupDetail(auth0Sub, groupId)
                }
            }
        }
    }

    @Nested
    inner class UpdateGroupStatus {
        private val groupId = UUID.randomUUID()

        @ParameterizedTest
        @EnumSource(UserJobStatus::class)
        fun `should update status and save`(targetStatus: UserJobStatus) {
            val group = TestFixtures.jobGroupEntity()
            val userJobGroup = TestFixtures.userJobGroupEntity(user = user, group = group, status = UserJobStatus.NEW)

            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userJobGroupFacade.findByUserIdAndGroupId(user.id, groupId) } returns userJobGroup
            every { userJobGroupFacade.save(userJobGroup) } returns userJobGroup

            val result = service.updateGroupStatus(auth0Sub, groupId, targetStatus)

            assertEquals(targetStatus, result.status)
            verify { userJobGroupFacade.save(userJobGroup) }
        }

        @Nested
        inner class ErrorHandling {
            @Test
            fun `should throw NotFoundException when user does not exist`() {
                every { userFacade.findByAuth0Sub(auth0Sub) } returns null

                assertThrows<NotFoundException> {
                    service.updateGroupStatus(auth0Sub, groupId, UserJobStatus.APPLIED)
                }
            }

            @Test
            fun `should throw NotFoundException when group not found for user`() {
                every { userFacade.findByAuth0Sub(auth0Sub) } returns user
                every { userJobGroupFacade.findByUserIdAndGroupId(user.id, groupId) } returns null

                assertThrows<NotFoundException> {
                    service.updateGroupStatus(auth0Sub, groupId, UserJobStatus.APPLIED)
                }
            }
        }
    }

    @Nested
    inner class BulkUpdateGroupStatus {
        @Test
        fun `should update all groups when all groupIds exist`() {
            val group1 = TestFixtures.jobGroupEntity()
            val group2 = TestFixtures.jobGroupEntity(title = "Java Developer")
            val userJobGroup1 = TestFixtures.userJobGroupEntity(user = user, group = group1, status = UserJobStatus.NEW)
            val userJobGroup2 = TestFixtures.userJobGroupEntity(user = user, group = group2, status = UserJobStatus.NEW)
            val groupIds = listOf(group1.id, group2.id)

            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userJobGroupFacade.findByUserIdAndGroupIds(user.id, groupIds) } returns
                listOf(userJobGroup1, userJobGroup2)
            every { userJobGroupFacade.saveAll(any()) } returns listOf(userJobGroup1, userJobGroup2)

            val result = service.bulkUpdateGroupStatus(auth0Sub, groupIds, UserJobStatus.IRRELEVANT)

            assertEquals(2, result.size)
            assertEquals(UserJobStatus.IRRELEVANT, userJobGroup1.status)
            assertEquals(UserJobStatus.IRRELEVANT, userJobGroup2.status)
            verify { userJobGroupFacade.saveAll(listOf(userJobGroup1, userJobGroup2)) }
        }

        @Nested
        inner class ErrorHandling {
            @Test
            fun `should throw NotFoundException when user does not exist`() {
                every { userFacade.findByAuth0Sub(auth0Sub) } returns null

                assertThrows<NotFoundException> {
                    service.bulkUpdateGroupStatus(auth0Sub, listOf(UUID.randomUUID()), UserJobStatus.APPLIED)
                }
            }

            @Test
            fun `should throw NotFoundException when some groupIds not found for user`() {
                val group = TestFixtures.jobGroupEntity()
                val userJobGroup = TestFixtures.userJobGroupEntity(user = user, group = group)
                val missingGroupId = UUID.randomUUID()
                val groupIds = listOf(group.id, missingGroupId)

                every { userFacade.findByAuth0Sub(auth0Sub) } returns user
                every { userJobGroupFacade.findByUserIdAndGroupIds(user.id, groupIds) } returns listOf(userJobGroup)

                val exception =
                    assertThrows<NotFoundException> {
                        service.bulkUpdateGroupStatus(auth0Sub, groupIds, UserJobStatus.APPLIED)
                    }
                assertTrue(exception.message!!.contains(missingGroupId.toString()))
            }
        }
    }
}
