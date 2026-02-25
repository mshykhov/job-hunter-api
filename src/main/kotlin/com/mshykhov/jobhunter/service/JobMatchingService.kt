package com.mshykhov.jobhunter.service

import com.mshykhov.jobhunter.persistence.facade.JobFacade
import com.mshykhov.jobhunter.persistence.facade.UserJobFacade
import com.mshykhov.jobhunter.persistence.facade.UserPreferenceFacade
import com.mshykhov.jobhunter.persistence.model.JobEntity
import com.mshykhov.jobhunter.persistence.model.UserJobEntity
import com.mshykhov.jobhunter.persistence.model.UserPreferenceEntity
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

private val logger = KotlinLogging.logger {}

@Service
class JobMatchingService(
    private val jobFacade: JobFacade,
    private val userPreferenceFacade: UserPreferenceFacade,
    private val userJobFacade: UserJobFacade,
    private val coldFilterService: ColdFilterService,
    private val clock: Clock,
) {
    @Scheduled(fixedDelayString = "\${jobhunter.matching.interval-ms:60000}")
    fun processUnmatchedJobs() {
        val jobs = jobFacade.findUnmatched()
        if (jobs.isEmpty()) return

        val preferences = userPreferenceFacade.findAll()
        if (preferences.isEmpty()) {
            markAllMatched(jobs)
            return
        }

        logger.info { "Matching ${jobs.size} jobs against ${preferences.size} user preferences" }

        for (job in jobs) {
            matchJobToUsers(job, preferences)
        }

        markAllMatched(jobs)
        logger.info { "Matching complete for ${jobs.size} jobs" }
    }

    @Transactional
    fun matchJobToUsers(
        job: JobEntity,
        preferences: List<UserPreferenceEntity>,
    ) {
        val matches = preferences.filter { coldFilterService.matches(job, it) }

        if (matches.isNotEmpty()) {
            val userJobs =
                matches.map { preference ->
                    UserJobEntity(
                        user = preference.user,
                        job = job,
                    )
                }
            userJobFacade.saveAll(userJobs)
            logger.debug { "Job '${job.title}' matched ${matches.size} users" }
        }
    }

    @Transactional
    fun markAllMatched(jobs: List<JobEntity>) {
        val now = Instant.now(clock)
        jobs.forEach { it.matchedAt = now }
        jobFacade.saveAll(jobs)
    }
}
