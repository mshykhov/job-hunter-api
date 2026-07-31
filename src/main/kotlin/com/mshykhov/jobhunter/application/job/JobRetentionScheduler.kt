package com.mshykhov.jobhunter.application.job

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class JobRetentionScheduler(private val jobRetentionService: JobRetentionService) {
    @Scheduled(fixedDelayString = "\${jobhunter.retention.interval-ms:86400000}")
    fun purgeExpiredJobs() {
        jobRetentionService.purgeExpiredJobs()
    }
}
