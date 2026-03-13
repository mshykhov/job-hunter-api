package com.mshykhov.jobhunter.application.userjob

import com.mshykhov.jobhunter.application.common.NotFoundException
import com.mshykhov.jobhunter.support.TestFixtures
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertSame

class UserJobFacadeTest {
    private val userJobRepository = mockk<UserJobRepository>()
    private val userJobGroupFacade = mockk<UserJobGroupFacade>()

    private val facade = UserJobFacade(userJobRepository, userJobGroupFacade)

    @Nested
    inner class FindOrCreateForGroupMember {
        @Test
        fun `should return existing user job when found`() {
            val user = TestFixtures.userEntity()
            val job = TestFixtures.jobEntity()
            val existingUserJob = TestFixtures.userJobEntity(user = user, job = job)

            every { userJobRepository.findByUserIdAndJobId(user.id, job.id) } returns existingUserJob

            val result = facade.findOrCreateForGroupMember(user, job.id)

            assertSame(existingUserJob, result)
            verify(exactly = 0) { userJobGroupFacade.findByUserIdAndJobId(any(), any()) }
        }

        @Test
        fun `should create user job when not found but group match exists`() {
            val user = TestFixtures.userEntity()
            val job = TestFixtures.jobEntity()
            val group = job.group
            val userJobGroup = TestFixtures.userJobGroupEntity(user = user, group = group)
            val groupWithJobs = mockk<com.mshykhov.jobhunter.application.job.JobGroupEntity>()

            every { userJobRepository.findByUserIdAndJobId(user.id, job.id) } returns null
            every { userJobGroupFacade.findByUserIdAndJobId(user.id, job.id) } returns
                userJobGroup.apply {
                    every { groupWithJobs.jobs } returns listOf(job)
                    // Use the real group from userJobGroup which has jobs loaded via @EntityGraph
                }

            // Since we can't easily set jobs on a real JobGroupEntity, let's use mockk
            val mockUserJobGroup = mockk<UserJobGroupEntity>()
            val mockGroup = mockk<com.mshykhov.jobhunter.application.job.JobGroupEntity>()
            every { mockGroup.jobs } returns listOf(job)
            every { mockUserJobGroup.group } returns mockGroup

            every { userJobRepository.findByUserIdAndJobId(user.id, job.id) } returns null
            every { userJobGroupFacade.findByUserIdAndJobId(user.id, job.id) } returns mockUserJobGroup
            every { userJobRepository.save(any()) } answers { firstArg() }

            val result = facade.findOrCreateForGroupMember(user, job.id)

            assertEquals(job, result.job)
            assertEquals(user, result.user)
            verify { userJobRepository.save(any()) }
        }

        @Test
        fun `should throw NotFoundException when no group match exists`() {
            val user = TestFixtures.userEntity()
            val jobId = TestFixtures.jobEntity().id

            every { userJobRepository.findByUserIdAndJobId(user.id, jobId) } returns null
            every { userJobGroupFacade.findByUserIdAndJobId(user.id, jobId) } returns null

            assertThrows<NotFoundException> {
                facade.findOrCreateForGroupMember(user, jobId)
            }
        }
    }
}
