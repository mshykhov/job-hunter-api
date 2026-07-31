package com.mshykhov.jobhunter.application.job

import com.mshykhov.jobhunter.infrastructure.metrics.MatchingMetrics
import com.mshykhov.jobhunter.infrastructure.retention.RetentionProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

private val logger = KotlinLogging.logger {}

@Service
class JobRetentionService(
    private val jobFacade: JobFacade,
    private val jobGroupFacade: JobGroupFacade,
    private val retentionProperties: RetentionProperties,
    private val matchingMetrics: MatchingMetrics,
    private val clock: Clock,
) {
    private val startedAt: Instant = Instant.now(clock)

    @Transactional
    fun purgeExpiredJobs(): Int {
        if (!retentionProperties.enabled) return 0

        val now = Instant.now(clock)
        if (now.isBefore(startedAt.plus(retentionProperties.gracePeriod))) return 0

        val threshold = now.minus(retentionProperties.retentionPeriod)
        val purgeableIds = jobFacade.findPurgeableIds(threshold, retentionProperties.batchSize)
        if (purgeableIds.isEmpty()) return 0

        val groupIds = jobFacade.findGroupIdsByIds(purgeableIds)
        jobFacade.deleteByIds(purgeableIds)

        val emptyGroupIds = jobGroupFacade.findIdsWithNoJobs(groupIds)
        if (emptyGroupIds.isNotEmpty()) jobGroupFacade.deleteByIds(emptyGroupIds)

        matchingMetrics.recordPurge(purgeableIds.size)
        logger.info { "Purged ${purgeableIds.size} jobs and ${emptyGroupIds.size} empty groups" }

        return purgeableIds.size
    }
}
