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
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration
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
    private val coldFilterChain = ColdFilterChain()

    fun processUnmatchedJobs() {
        val jobs = jobFacade.findUnmatched()
        if (jobs.isEmpty()) return

        val preferences = userPreferenceFacade.findAll()
        if (preferences.isEmpty()) {
            markMatched(jobs)
            return
        }

        val userChatClients = buildUserChatClients(preferences)

        logger.info {
            "Matching ${jobs.size} jobs against ${preferences.size} user preferences " +
                "(${userChatClients.size} AI-enabled)"
        }

        val semaphore = Semaphore(aiProperties.matching.concurrency)
        val results =
            runBlocking(Dispatchers.IO) {
                jobs
                    .map { job ->
                        async {
                            semaphore.withPermit {
                                processJob(job, preferences, userChatClients)
                            }
                        }
                    }.awaitAll()
            }

        val successResults = results.filterIsInstance<MatchResult.Success>()
        val matched = successResults.map { it.job }
        val failedCount = results.count { it is MatchResult.Failure }
        val totalStats = successResults.fold(MatchingStats()) { acc, r -> acc.merge(r.stats) }

        if (matched.isNotEmpty()) markMatched(matched)

        logger.info {
            "Matching complete: ${matched.size}/${jobs.size} processed, $failedCount failed — ${totalStats.summary()}"
        }
        // TODO: [REPORT] if totalStats.aiFailed > threshold or failedCount > 0, notify admin
    }

    private fun processJob(
        job: JobEntity,
        preferences: List<UserPreferenceEntity>,
        userChatClients: Map<UUID, ChatClient>,
    ): MatchResult =
        try {
            val stats = MatchingStats()
            val userJobs = matchJobToUsers(job, preferences, userChatClients, stats)
            if (userJobs.isNotEmpty()) {
                userJobFacade.saveAll(userJobs)
                stats.saved += userJobs.size
                logger.info {
                    "Job '${job.title}' matched to ${userJobs.size} users (scores: ${userJobs.map { it.aiRelevanceScore }})"
                }
            }
            MatchResult.Success(job, stats)
        } catch (e: Exception) {
            logger.error(e) { "Matching failed for job '${job.title}'" }
            // TODO: [REPORT] persistent failures may indicate API key expiry or rate limit
            MatchResult.Failure
        }

    private fun matchJobToUsers(
        job: JobEntity,
        preferences: List<UserPreferenceEntity>,
        userChatClients: Map<UUID, ChatClient>,
        stats: MatchingStats,
    ): List<UserJobEntity> {
        val existingByUserId = userJobFacade.findByJobId(job.id).associateBy { it.user.id }
        val userJobs = mutableListOf<UserJobEntity>()

        for (preference in preferences) {
            val filterResult = coldFilterChain.evaluate(job, preference)
            if (filterResult is FilterResult.Rejected) {
                logger.debug {
                    "Job '${job.title}' rejected for user ${preference.user.id} " +
                        "by [${filterResult.filter}]: ${filterResult.reason}"
                }
                stats.coldRejected++
                continue
            }

            val existing = existingByUserId[preference.user.id]
            val chatClient = userChatClients[preference.user.id]

            if (chatClient != null) {
                val result = evaluateWithAi(job, preference, chatClient, stats, existing)
                if (result != null) userJobs += result
            } else {
                stats.coldOnly++
                userJobs += existing?.apply {
                    aiRelevanceScore = 0
                    aiReasoning = COLD_ONLY_REASONING
                    aiInferredRemote = null
                } ?: UserJobEntity(
                    user = preference.user,
                    job = job,
                    aiRelevanceScore = 0,
                    aiReasoning = COLD_ONLY_REASONING,
                )
            }
        }

        return userJobs
    }

    private fun evaluateWithAi(
        job: JobEntity,
        preference: UserPreferenceEntity,
        chatClient: ChatClient,
        stats: MatchingStats,
        existing: UserJobEntity?,
    ): UserJobEntity? {
        val aiResult =
            try {
                jobRelevanceEvaluator.evaluate(job, preference, chatClient)
            } catch (e: Exception) {
                logger.warn(e) { "AI evaluation failed for job '${job.title}', user ${preference.user.id}" }
                // TODO: [REPORT] AI call failure — may need admin attention
                stats.aiFailed++
                return null
            }
        stats.aiEvaluated++

        backfillRemoteIfNeeded(job, aiResult.inferredRemote)

        if (preference.search.remoteOnly && !aiResult.inferredRemote) {
            logger.debug {
                "Job '${job.title}' post-AI rejected for user ${preference.user.id}: " +
                    "remoteOnly but inferredRemote=false"
            }
            stats.postAiRejected++
            return null
        }

        return existing?.apply {
            aiRelevanceScore = aiResult.score
            aiReasoning = aiResult.reasoning
            aiInferredRemote = aiResult.inferredRemote
        } ?: UserJobEntity(
            user = preference.user,
            job = job,
            aiRelevanceScore = aiResult.score,
            aiReasoning = aiResult.reasoning,
            aiInferredRemote = aiResult.inferredRemote,
        )
    }

    private fun backfillRemoteIfNeeded(
        job: JobEntity,
        inferredRemote: Boolean,
    ) {
        if (job.remote != null) return
        job.remote = inferredRemote
        jobFacade.updateRemote(job.id, inferredRemote)
        logger.debug { "Backfilled remote=$inferredRemote for job '${job.title}'" }
    }

    private fun buildUserChatClients(preferences: List<UserPreferenceEntity>): Map<UUID, ChatClient> =
        preferences
            .filter { it.matching.matchWithAi }
            .map { it.user.id }
            .distinct()
            .mapNotNull { userId ->
                val settings = userAiSettingsFacade.findByUserId(userId)
                if (settings == null) {
                    logger.warn { "User $userId has matchWithAi=true but no AI settings — falling back to cold-only" }
                    // TODO: [REPORT] user enabled AI matching without API key — notify user
                    null
                } else {
                    userId to chatClientFactory.createForUser(settings)
                }
            }.toMap()

    @Transactional
    fun rematch(since: Instant?): Int {
        val maxSince = Instant.now(clock).minus(MAX_REMATCH_PERIOD)
        val effectiveSince =
            when {
                since == null -> maxSince
                since.isBefore(maxSince) -> maxSince
                else -> since
            }
        val jobs = jobFacade.findMatchedSince(effectiveSince)
        if (jobs.isEmpty()) return 0

        jobFacade.updateMatchedAt(jobs.map { it.id }, null)

        logger.info { "Rematch queued: ${jobs.size} jobs reset (since=$effectiveSince)" }
        return jobs.size
    }

    private fun markMatched(jobs: List<JobEntity>) {
        jobFacade.updateMatchedAt(jobs.map { it.id }, Instant.now(clock))
    }

    private sealed interface MatchResult {
        data class Success(
            val job: JobEntity,
            val stats: MatchingStats,
        ) : MatchResult

        data object Failure : MatchResult
    }

    companion object {
        private val MAX_REMATCH_PERIOD = Duration.ofDays(3)
        private const val COLD_ONLY_REASONING = "Cold filter match only — AI evaluation disabled"
    }
}
