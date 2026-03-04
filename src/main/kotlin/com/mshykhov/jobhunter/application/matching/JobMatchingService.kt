package com.mshykhov.jobhunter.application.matching

import com.mshykhov.jobhunter.application.ai.ChatClientFactory
import com.mshykhov.jobhunter.application.ai.JobRelevanceEvaluator
import com.mshykhov.jobhunter.application.ai.UserAiSettingsFacade
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
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.springframework.ai.chat.client.ChatClient
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class JobMatchingService(
    private val jobFacade: JobFacade,
    private val userPreferenceFacade: UserPreferenceFacade,
    private val userJobFacade: UserJobFacade,
    private val userAiSettingsFacade: UserAiSettingsFacade,
    private val jobRelevanceEvaluator: JobRelevanceEvaluator,
    private val chatClientFactory: ChatClientFactory,
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

        val userChatClients = buildUserChatClients(preferences)
        val eligiblePreferences = preferences.filter { it.user.id in userChatClients }
        if (eligiblePreferences.isEmpty()) {
            markMatched(jobs)
            return
        }

        logger.info { "Matching ${jobs.size} jobs against ${eligiblePreferences.size} user preferences" }

        val semaphore = Semaphore(aiProperties.matching.concurrency)
        val results =
            runBlocking(Dispatchers.IO) {
                jobs
                    .map { job ->
                        async {
                            semaphore.withPermit {
                                try {
                                    matchJobToUsers(job, eligiblePreferences, userChatClients)
                                    MatchResult.Success(job)
                                } catch (e: Exception) {
                                    logger.error(e) { "AI evaluation failed for job '${job.title}', skipping" }
                                    MatchResult.Failure
                                }
                            }
                        }
                    }.awaitAll()
            }

        val matched = results.filterIsInstance<MatchResult.Success>().map { it.job }
        val failedCount = results.count { it is MatchResult.Failure }

        if (matched.isNotEmpty()) {
            markMatched(matched)
        }
        logger.info { "Matching complete: ${matched.size} processed, $failedCount failed" }
    }

    private fun buildUserChatClients(preferences: List<UserPreferenceEntity>): Map<UUID, ChatClient> =
        preferences
            .filter { it.matching.matchWithAi }
            .map { it.user.id }
            .distinct()
            .mapNotNull { userId ->
                val settings = userAiSettingsFacade.findByUserId(userId)
                if (settings == null) {
                    logger.warn { "User $userId has no AI settings, skipping in matching" }
                    null
                } else {
                    userId to chatClientFactory.createForUser(settings)
                }
            }.toMap()

    private fun matchJobToUsers(
        job: JobEntity,
        preferences: List<UserPreferenceEntity>,
        userChatClients: Map<UUID, ChatClient>,
    ) {
        val coldMatches = preferences.filter { coldFilterMatches(job, it) }
        if (coldMatches.isEmpty()) return

        val existingUserIds = userJobFacade.findUserIdsByJobId(job.id)
        val newMatches = coldMatches.filter { it.user.id !in existingUserIds }
        if (newMatches.isEmpty()) return

        val userJobs =
            newMatches.mapNotNull { preference ->
                val chatClient = userChatClients[preference.user.id] ?: return@mapNotNull null
                val aiResult = jobRelevanceEvaluator.evaluate(job, preference, chatClient)
                if (preference.search.remoteOnly && !aiResult.inferredRemote) return@mapNotNull null
                UserJobEntity(
                    user = preference.user,
                    job = job,
                    aiRelevanceScore = aiResult.score,
                    aiReasoning = aiResult.reasoning,
                    aiInferredRemote = aiResult.inferredRemote,
                )
            }

        if (userJobs.isEmpty()) return
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
        jobFacade.updateMatchedAt(jobIds, null)

        logger.info { "Rematch queued: ${jobs.size} jobs reset (since=${since ?: "all"})" }
        return jobs.size
    }

    @Transactional
    private fun markMatched(jobs: List<JobEntity>) {
        val ids = jobs.map { it.id }
        jobFacade.updateMatchedAt(ids, Instant.now(clock))
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
            hasNoExcludedTitleKeywords(job, preference) &&
            isCompanyAllowed(job, preference) &&
            matchesCategories(job, preference)

    private fun isSourceAllowed(
        job: JobEntity,
        preference: UserPreferenceEntity,
    ): Boolean {
        if (preference.matching.disabledSources.isEmpty()) return true
        return job.source !in preference.matching.disabledSources
    }

    private fun isRemoteMatch(
        job: JobEntity,
        preference: UserPreferenceEntity,
    ): Boolean {
        if (!preference.search.remoteOnly) return true
        return job.remote != false
    }

    private fun hasNoExcludedKeywords(
        job: JobEntity,
        preference: UserPreferenceEntity,
    ): Boolean {
        if (preference.matching.excludedKeywords.isEmpty()) return true
        val searchText = buildSearchText(job)
        return preference.matching.excludedKeywords.none { keyword ->
            searchText.contains(keyword.lowercase())
        }
    }

    private fun hasNoExcludedTitleKeywords(
        job: JobEntity,
        preference: UserPreferenceEntity,
    ): Boolean {
        if (preference.matching.excludedTitleKeywords.isEmpty()) return true
        val titleText = job.title.lowercase()
        return preference.matching.excludedTitleKeywords.none { keyword ->
            titleText.contains(keyword.lowercase())
        }
    }

    private fun isCompanyAllowed(
        job: JobEntity,
        preference: UserPreferenceEntity,
    ): Boolean {
        if (preference.matching.excludedCompanies.isEmpty()) return true
        val company = job.company?.lowercase() ?: return true
        return preference.matching.excludedCompanies.none { excluded ->
            company.contains(excluded.lowercase())
        }
    }

    private fun matchesCategories(
        job: JobEntity,
        preference: UserPreferenceEntity,
    ): Boolean {
        if (preference.search.categories.isEmpty()) return true
        val searchText = buildSearchText(job)
        return preference.search.categories.any { category ->
            searchText.contains(category.lowercase())
        }
    }

    private fun buildSearchText(job: JobEntity): String =
        listOfNotNull(job.title, job.company, job.description, job.location, job.salary)
            .joinToString(" ")
            .lowercase()
}
