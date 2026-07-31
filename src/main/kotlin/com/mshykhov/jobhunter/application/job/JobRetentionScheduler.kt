package com.mshykhov.jobhunter.application.job

import com.mshykhov.jobhunter.infrastructure.metrics.MatchingMetrics
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class JobRetentionScheduler(private val jobRetentionService: JobRetentionService, private val matchingMetrics: MatchingMetrics) {
    @Scheduled(fixedDelayString = "\${jobhunter.retention.interval-ms:86400000}")
    fun purgeExpiredJobs() {
        try {
            val summary = jobRetentionService.purgeExpiredJobs()
            if (summary.jobsDeleted > 0) {
                matchingMetrics.recordPurge(summary.jobsDeleted)
                logger.info { "Purged ${summary.jobsDeleted} jobs and ${summary.groupsDeleted} empty groups" }
            }
        } catch (e: Exception) {
            logger.error(e) { "Job retention purge run failed unexpectedly" }
        }
    }
}
