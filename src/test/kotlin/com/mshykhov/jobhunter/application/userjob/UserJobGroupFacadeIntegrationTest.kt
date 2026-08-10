package com.mshykhov.jobhunter.application.userjob

import com.mshykhov.jobhunter.application.job.JobGroupRepository
import com.mshykhov.jobhunter.application.user.UserRepository
import com.mshykhov.jobhunter.support.AbstractIntegrationTest
import com.mshykhov.jobhunter.support.TestFixtures
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserJobGroupFacadeIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var userJobGroupFacade: UserJobGroupFacade

    @Autowired
    lateinit var userJobGroupRepository: UserJobGroupRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var jobGroupRepository: JobGroupRepository

    @Test
    fun `should not fetch a reviewed group from a stale NEW id snapshot`() {
        val user = userRepository.save(TestFixtures.userEntity())
        val group = jobGroupRepository.save(TestFixtures.jobGroupEntity(title = "Reviewed after snapshot ${System.nanoTime()}"))
        val userGroup = userJobGroupRepository.save(TestFixtures.userJobGroupEntity(user = user, group = group))
        val snapshotIds = userJobGroupFacade.findIdsByUserIdAndStatus(user.id, UserJobStatus.NEW)

        userGroup.status = UserJobStatus.APPLIED
        userJobGroupRepository.saveAndFlush(userGroup)

        val result = userJobGroupFacade.findByIdsWithGroupAndJobs(user.id, UserJobStatus.NEW, snapshotIds)

        assertFalse(result.any { it.id == userGroup.id })
    }

    @Test
    fun `should not delete a group reviewed after chunk fetch`() {
        val user = userRepository.save(TestFixtures.userEntity())
        val group = jobGroupRepository.save(TestFixtures.jobGroupEntity(title = "Reviewed before delete ${System.nanoTime()}"))
        val userGroup = userJobGroupRepository.save(TestFixtures.userJobGroupEntity(user = user, group = group))
        val fetched = userJobGroupFacade.findByIdsWithGroupAndJobs(user.id, UserJobStatus.NEW, listOf(userGroup.id))
        assertTrue(fetched.any { it.id == userGroup.id })

        userGroup.status = UserJobStatus.IRRELEVANT
        userJobGroupRepository.saveAndFlush(userGroup)

        val deleted =
            userJobGroupFacade.deleteByIdsAndUserIdAndStatus(
                listOf(userGroup.id),
                user.id,
                UserJobStatus.NEW,
            )

        assertEquals(0, deleted)
        assertTrue(userJobGroupRepository.existsById(userGroup.id))
    }
}
