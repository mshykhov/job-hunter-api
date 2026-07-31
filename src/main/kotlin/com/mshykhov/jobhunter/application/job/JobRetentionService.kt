package com.mshykhov.jobhunter.application.job

import com.mshykhov.jobhunter.infrastructure.retention.RetentionAnchor
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
    private val retentionAnchor: RetentionAnchor,
    private val clock: Clock,
) {
    @Transactional
    fun purgeExpiredJobs(): PurgeSummary {
        if (!retentionProperties.enabled) {
            logger.info { "Retention purge skipped: disabled" }
            return PurgeSummary(0, 0)
        }

        val now = Instant.now(clock)
        val graceUntil = retentionAnchor.instant().plus(retentionProperties.gracePeriod)
        if (now.isBefore(graceUntil)) {
            logger.info { "Retention purge skipped: grace period active until $graceUntil" }
            return PurgeSummary(0, 0)
        }

        val threshold = now.minus(retentionProperties.retentionPeriod)
        var jobsDeleted = 0
        var groupsDeleted = 0

        while (jobsDeleted < retentionProperties.maxPerRun) {
            val batchLimit = minOf(retentionProperties.batchSize, retentionProperties.maxPerRun - jobsDeleted)
            val purgeableIds = jobFacade.findPurgeableIds(threshold, batchLimit)
            if (purgeableIds.isEmpty()) break

            val groupIds = jobFacade.findGroupIdsByIds(purgeableIds)
            jobFacade.deleteByIds(purgeableIds)

            val emptyGroupIds = jobGroupFacade.findIdsWithNoJobs(groupIds)
            if (emptyGroupIds.isNotEmpty()) jobGroupFacade.deleteByIds(emptyGroupIds)

            jobsDeleted += purgeableIds.size
            groupsDeleted += emptyGroupIds.size

            if (purgeableIds.size < batchLimit) break
        }

        return PurgeSummary(jobsDeleted, groupsDeleted)
    }
}
