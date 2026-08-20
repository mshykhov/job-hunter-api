package com.mshykhov.jobhunter.application.statistics

import com.mshykhov.jobhunter.application.job.JobGroupRepository
import com.mshykhov.jobhunter.application.job.JobRepository
import com.mshykhov.jobhunter.application.job.JobSource
import com.mshykhov.jobhunter.application.user.UserRepository
import com.mshykhov.jobhunter.support.AbstractIntegrationTest
import com.mshykhov.jobhunter.support.TestFixtures
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Timestamp
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class VacancyStatisticsServiceIntegrationTest : AbstractIntegrationTest() {
    @Autowired lateinit var service: VacancyStatisticsService

    @Autowired lateinit var userRepository: UserRepository

    @Autowired lateinit var groupRepository: JobGroupRepository

    @Autowired lateinit var jobRepository: JobRepository

    @Autowired lateinit var decisionRepository: UserJobGroupDecisionRepository

    @Autowired lateinit var decisionFacade: UserJobGroupDecisionFacade

    @Autowired lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `should aggregate groups and personalized outcomes without double counting sources`() {
        val suffix = System.nanoTime()
        val user = userRepository.save(TestFixtures.userEntity("statistics-$suffix"))
        val other = userRepository.save(TestFixtures.userEntity("statistics-other-$suffix"))
        val start = Instant.parse("2090-02-01T00:00:00Z")
        val cold = group("Cold $suffix", start, JobSource.DOU, JobSource.DJINNI)
        val remote = group("Remote $suffix", start.plusSeconds(86_400), JobSource.DOU)
        val scoredOne = group("Scored one $suffix", start.plusSeconds(86_400), JobSource.DOU)
        val scoredTwo = group("Scored two $suffix", start.plusSeconds(2 * 86_400), JobSource.DJINNI)
        val noDecision = group("No decision $suffix", start.plusSeconds(2 * 86_400), JobSource.DOU)
        val retained = group("Retained $suffix", start.plusSeconds(2 * 86_400))
        decision(user, cold, start, DecisionOutcome.COLD_REJECTED, null, arrayOf("DOU", "DJINNI"))
        decision(user, remote, start.plusSeconds(86_400), DecisionOutcome.AI_REJECTED_REMOTE, 40, arrayOf("DOU"))
        decision(user, scoredOne, start.plusSeconds(86_400), DecisionOutcome.AI_SCORED, 70, arrayOf("DOU"))
        decision(user, scoredTwo, start.plusSeconds(2 * 86_400), DecisionOutcome.AI_SCORED, 90, arrayOf("DJINNI"))
        decision(user, retained, start.plusSeconds(2 * 86_400), DecisionOutcome.LEGACY_REJECTED_UNKNOWN, null, arrayOf("DOU"))
        decision(other, cold, start, DecisionOutcome.AI_SCORED, 100, arrayOf("DOU"))

        val result = service.query(user.auth0Sub, VacancyStatisticsQuery(start, start.plusSeconds(3 * 86_400), StatisticsBucket.DAY, null))

        assertEquals(3, result.points.size)
        assertEquals(1, result.points[0].coldRejected)
        assertEquals(0, result.points[0].aiScored)
        assertEquals(2, result.points[1].allVacancies)
        assertEquals(1, result.points[1].notFullyRemote)
        assertEquals(1, result.points[1].aiScored)
        assertEquals(3, result.points[2].allVacancies)
        assertEquals(1, result.points[2].legacyRejectedUnknown)
        assertEquals(90.0, result.points[2].medianScore)
        assertNotNull(result.exactSince)
        assertNotNull(result.sourceCoverageSince)

        val dou = service.query(user.auth0Sub, VacancyStatisticsQuery(start, start.plusSeconds(3 * 86_400), StatisticsBucket.DAY, listOf(JobSource.DOU)))
        assertEquals(1, dou.points[0].allVacancies)
        assertEquals(1, dou.points[2].legacyRejectedUnknown)
        assertEquals(2, dou.points[2].allVacancies) // retained ledger source and no-decision DOU group are distinct
        assertNull(dou.points[0].medianScore)
    }

    @Test
    fun `should clamp epoch and zero fill weekly and monthly buckets`() {
        val suffix = System.nanoTime()
        val user = userRepository.save(TestFixtures.userEntity("statistics-clamp-$suffix"))
        val start = Instant.parse("2091-01-10T00:00:00Z")
        val group = group("Clamp $suffix", start, JobSource.DOU)
        decision(user, group, start, DecisionOutcome.COLD_ONLY, null, arrayOf("DOU"))

        val week = service.query(user.auth0Sub, VacancyStatisticsQuery(start, Instant.parse("2091-02-01T00:00:00Z"), StatisticsBucket.WEEK, null))
        val month = service.query(user.auth0Sub, VacancyStatisticsQuery(Instant.EPOCH, Instant.parse("2000-01-01T00:00:00Z"), StatisticsBucket.MONTH, null))

        assertEquals(start, week.from)
        assertEquals(true, week.points.any { it.allVacancies == 0L })
        assertEquals(emptyList(), month.points)
    }

    @Test
    fun `should upsert one decision preserve vacancy seen time and cascade with user`() {
        val suffix = System.nanoTime()
        val user = userRepository.save(TestFixtures.userEntity("statistics-upsert-$suffix"))
        val group =
            requireNotNull(
                groupRepository.findById(group("Upsert $suffix", Instant.parse("2093-01-01T00:00:00Z"), JobSource.DOU).id).orElse(null),
            )
        val job = jobRepository.findByGroupId(group.id).single()

        decisionFacade.upsert(user, group, listOf(job), DecisionOutcome.COLD_REJECTED, coldFilter = "remote")
        val first = requireNotNull(decisionRepository.findByUserIdAndGroupId(user.id, group.id))
        decisionFacade.upsert(user, group, listOf(job), DecisionOutcome.AI_SCORED, aiScore = 85, inferredRemote = true)
        val second = requireNotNull(decisionRepository.findByUserIdAndGroupId(user.id, group.id))

        assertEquals(first.vacancySeenAt, second.vacancySeenAt)
        assertEquals(DecisionOutcome.AI_SCORED, second.outcome)
        userRepository.delete(user)
        assertEquals(null, decisionRepository.findByUserIdAndGroupId(user.id, group.id))
    }

    private fun group(title: String, createdAt: Instant, vararg sources: JobSource) =
        groupRepository.save(TestFixtures.jobGroupEntity(title = title)).also { group ->
            sources.forEachIndexed { index, source ->
                jobRepository.save(TestFixtures.jobEntity(title = title, group = group, source = source, url = "https://example.com/stat-$title-$index"))
            }
            jdbcTemplate.update("UPDATE job_groups SET created_at = ? WHERE id = ?", Timestamp.from(createdAt), group.id)
        }

    private fun decision(
        user: com.mshykhov.jobhunter.application.user.UserEntity,
        group: com.mshykhov.jobhunter.application.job.JobGroupEntity,
        seenAt: Instant,
        outcome: DecisionOutcome,
        score: Int?,
        sources: Array<String>,
    ) {
        decisionRepository.save(
            UserJobGroupDecisionEntity(
                user = user,
                group = group,
                vacancySeenAt = seenAt,
                decidedAt = seenAt,
                outcome = outcome,
                aiScore = score,
                sources = sources,
            ),
        )
    }
}
