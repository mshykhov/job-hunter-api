package com.mshykhov.jobhunter.application.matching

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class JobMatchingScheduler(private val jobMatchingService: JobMatchingService) {
    @Scheduled(fixedDelayString = "\${jobhunter.matching.interval-ms:60000}")
    fun processUnmatchedJobs() {
        jobMatchingService.processUnmatchedJobs()
    }
}
