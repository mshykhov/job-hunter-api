package com.mshykhov.jobhunter.application.matching

import com.mshykhov.jobhunter.application.ai.JobRelevanceEvaluator
import com.mshykhov.jobhunter.application.job.JobEntity
import com.mshykhov.jobhunter.application.job.JobFacade
import com.mshykhov.jobhunter.application.preference.UserPreferenceEntity
import com.mshykhov.jobhunter.application.preference.UserPreferenceFacade
import com.mshykhov.jobhunter.application.userjob.UserJobEntity
import com.mshykhov.jobhunter.application.userjob.UserJobFacade
import com.mshykhov.jobhunter.infrastructure.ai.AiProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val logger = KotlinLogging.logger {}

@Service
class JobMatchingService(
    private val jobFacade: JobFacade,
    private val userPreferenceFacade: UserPreferenceFacade,
    private val userJobFacade: UserJobFacade,
    private val jobRelevanceEvaluator: JobRelevanceEvaluator,
    private val aiProperties: AiProperties,
    private val clock: Clock,
) {
    @Scheduled(fixedDelayString = "\${jobhunter.matching.interval-ms:60000}")
    fun processUnmatchedJobs() {
        val jobs = jobFacade.findUnmatched()
        if (jobs.isEmpty()) return

        val preferences = userPreferenceFacade.findAll()
        if (preferences.isEmpty()) {
            markMatched(jobs)
            return
        }

        logger.info { "Matching ${jobs.size} jobs against ${preferences.size} user preferences" }

        val batchSize = aiProperties.matching.batchSize
        val results =
            runBlocking(Dispatchers.IO) {
                jobs.chunked(batchSize).flatMap { batch ->
                    batch
                        .map { job ->
                            async {
                                try {
                                    matchJobToUsers(job, preferences)
                                    MatchResult.Success(job)
                                } catch (e: Exception) {
                                    logger.error(e) { "AI evaluation failed for job '${job.title}', skipping" }
                                    MatchResult.Failure
                                }
                            }
                        }.awaitAll()
                }
            }

        val matched = results.filterIsInstance<MatchResult.Success>().map { it.job }
        val failedCount = results.count { it is MatchResult.Failure }

        if (matched.isNotEmpty()) {
            markMatched(matched)
        }
        logger.info { "Matching complete: ${matched.size} processed, $failedCount failed" }
    }

    private fun matchJobToUsers(
        job: JobEntity,
        preferences: List<UserPreferenceEntity>,
    ) {
        val coldMatches = preferences.filter { coldFilterMatches(job, it) }
        if (coldMatches.isEmpty()) return

        val userJobs =
            coldMatches.map { preference ->
                val aiResult = jobRelevanceEvaluator.evaluate(job, preference)
                UserJobEntity(
                    user = preference.user,
                    job = job,
                    aiRelevanceScore = aiResult.score,
                    aiReasoning = aiResult.reasoning,
                )
            }

        userJobFacade.saveAll(userJobs)
        logger.info { "Job '${job.title}' evaluated for ${userJobs.size} users (scores: ${userJobs.map { it.aiRelevanceScore }})" }
    }

    @Transactional
    fun rematch(since: Instant?): Int {
        val jobs =
            if (since != null) {
                jobFacade.findMatchedSince(since)
            } else {
                jobFacade.findAllMatched()
            }
        if (jobs.isEmpty()) return 0

        val jobIds = jobs.map { it.id }
        userJobFacade.deleteByJobIds(jobIds)
        jobs.forEach { it.matchedAt = null }
        jobFacade.saveAll(jobs)

        logger.info { "Rematch queued: ${jobs.size} jobs reset (since=${since ?: "all"})" }
        return jobs.size
    }

    private fun markMatched(jobs: List<JobEntity>) {
        val now = Instant.now(clock)
        jobs.forEach { it.matchedAt = now }
        jobFacade.saveAll(jobs)
    }

    private sealed interface MatchResult {
        data class Success(
            val job: JobEntity,
        ) : MatchResult

        data object Failure : MatchResult
    }

    // --- Cold filter ---

    private fun coldFilterMatches(
        job: JobEntity,
        preference: UserPreferenceEntity,
    ): Boolean =
        isSourceAllowed(job, preference) &&
            isRemoteMatch(job, preference) &&
            hasNoExcludedKeywords(job, preference) &&
            matchesCategories(job, preference)

    private fun isSourceAllowed(
        job: JobEntity,
        preference: UserPreferenceEntity,
    ): Boolean {
        if (preference.disabledSources.isEmpty()) return true
        return job.source !in preference.disabledSources
    }

    private fun isRemoteMatch(
        job: JobEntity,
        preference: UserPreferenceEntity,
    ): Boolean {
        if (!preference.remoteOnly) return true
        return job.remote
    }

    private fun hasNoExcludedKeywords(
        job: JobEntity,
        preference: UserPreferenceEntity,
    ): Boolean {
        if (preference.excludedKeywords.isEmpty()) return true
        val searchText = buildSearchText(job)
        return preference.excludedKeywords.none { keyword ->
            searchText.contains(keyword.lowercase())
        }
    }

    private fun matchesCategories(
        job: JobEntity,
        preference: UserPreferenceEntity,
    ): Boolean {
        if (preference.categories.isEmpty()) return true
        val searchText = buildSearchText(job)
        return preference.categories.any { category ->
            searchText.contains(category.lowercase())
        }
    }

    private fun buildSearchText(job: JobEntity): String =
        listOfNotNull(job.title, job.company, job.description, job.location, job.salary)
            .joinToString(" ")
            .lowercase()
}
