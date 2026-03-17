package com.mshykhov.jobhunter.application.matching

import com.mshykhov.jobhunter.application.ai.AiUseCase
import com.mshykhov.jobhunter.application.ai.ChatClientFactory
import com.mshykhov.jobhunter.application.ai.JobRelevanceEvaluator
import com.mshykhov.jobhunter.application.ai.UserAiSettingsFacade
import com.mshykhov.jobhunter.application.job.JobEntity
import com.mshykhov.jobhunter.application.job.JobFacade
import com.mshykhov.jobhunter.application.job.JobGroupEntity
import com.mshykhov.jobhunter.application.preference.UserPreferenceEntity
import com.mshykhov.jobhunter.application.preference.UserPreferenceFacade
import com.mshykhov.jobhunter.application.userjob.UserJobGroupEntity
import com.mshykhov.jobhunter.application.userjob.UserJobGroupFacade
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
    private val userJobGroupFacade: UserJobGroupFacade,
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

        val jobsByGroup = jobs.groupBy { it.group }
        val userChatClients = buildUserChatClients(preferences)

        logger.info {
            "Matching ${jobs.size} jobs (${jobsByGroup.size} groups) against ${preferences.size} user preferences " +
                "(${userChatClients.size} AI-enabled)"
        }

        val semaphore = Semaphore(aiProperties.matching.concurrency)
        val results =
            runBlocking(Dispatchers.IO) {
                jobsByGroup.entries
                    .map { (group, groupJobs) ->
                        async {
                            semaphore.withPermit {
                                processGroup(group, groupJobs, preferences, userChatClients)
                            }
                        }
                    }.awaitAll()
            }

        val successResults = results.filterIsInstance<MatchResult.Success>()
        val matchedJobs = successResults.flatMap { it.jobs }
        val failedCount = results.count { it is MatchResult.Failure }
        val totalStats = successResults.fold(MatchingStats()) { acc, r -> acc.merge(r.stats) }

        if (matchedJobs.isNotEmpty()) markMatched(matchedJobs)

        logger.info {
            "Matching complete: ${matchedJobs.size}/${jobs.size} processed ($failedCount failed) — ${totalStats.summary()}"
        }
    }

    private fun processGroup(
        group: JobGroupEntity,
        groupJobs: List<JobEntity>,
        preferences: List<UserPreferenceEntity>,
        userChatClients: Map<UUID, ChatClient>,
    ): MatchResult =
        try {
            val stats = MatchingStats()
            val representative = selectRepresentative(groupJobs)
            val userJobGroups = matchGroupToUsers(group, representative, preferences, userChatClients, stats)
            if (userJobGroups.isNotEmpty()) {
                userJobGroupFacade.saveAll(userJobGroups)
                stats.saved += userJobGroups.size
                logger.info {
                    "Group '${group.title}' (${groupJobs.size} jobs) matched to ${userJobGroups.size} users " +
                        "(scores: ${userJobGroups.map { it.aiRelevanceScore }})"
                }
            }
            MatchResult.Success(groupJobs, stats)
        } catch (e: Exception) {
            logger.error(e) { "Matching failed for group '${group.title}'" }
            MatchResult.Failure
        }

    private fun selectRepresentative(groupJobs: List<JobEntity>): JobEntity = groupJobs.maxBy { it.description.length }

    private fun matchGroupToUsers(
        group: JobGroupEntity,
        representative: JobEntity,
        preferences: List<UserPreferenceEntity>,
        userChatClients: Map<UUID, ChatClient>,
        stats: MatchingStats,
    ): List<UserJobGroupEntity> {
        val existingByUserId = userJobGroupFacade.findByGroupId(group.id).associateBy { it.user.id }
        val userJobGroups = mutableListOf<UserJobGroupEntity>()

        for (preference in preferences) {
            val filterResult = coldFilterChain.evaluate(representative, preference)
            if (filterResult is FilterResult.Rejected) {
                logger.debug {
                    "Group '${group.title}' rejected for user ${preference.user.id} " +
                        "by [${filterResult.filter}]: ${filterResult.reason}"
                }
                stats.coldRejected++
                continue
            }

            val existing = existingByUserId[preference.user.id]
            val chatClient = userChatClients[preference.user.id]

            if (chatClient != null) {
                val result = evaluateWithAi(group, representative, preference, chatClient, stats, existing)
                if (result != null) userJobGroups += result
            } else {
                stats.coldOnly++
                userJobGroups += existing?.apply {
                    aiRelevanceScore = 0
                    aiReasoning = COLD_ONLY_REASONING
                } ?: UserJobGroupEntity(
                    user = preference.user,
                    group = group,
                    aiRelevanceScore = 0,
                    aiReasoning = COLD_ONLY_REASONING,
                )
            }
        }

        return userJobGroups
    }

    private fun evaluateWithAi(
        group: JobGroupEntity,
        representative: JobEntity,
        preference: UserPreferenceEntity,
        chatClient: ChatClient,
        stats: MatchingStats,
        existing: UserJobGroupEntity?,
    ): UserJobGroupEntity? {
        val aiResult =
            try {
                jobRelevanceEvaluator.evaluate(representative, preference, chatClient)
            } catch (e: Exception) {
                logger.warn(e) { "AI evaluation failed for group '${group.title}', user ${preference.user.id}" }
                stats.aiFailed++
                return null
            }
        stats.aiEvaluated++

        backfillRemoteIfNeeded(representative, aiResult.inferredRemote)

        if (preference.search.remoteOnly && !aiResult.inferredRemote) {
            logger.debug {
                "Group '${group.title}' post-AI rejected for user ${preference.user.id}: " +
                    "remoteOnly but inferredRemote=false"
            }
            stats.postAiRejected++
            return null
        }

        return existing?.apply {
            aiRelevanceScore = aiResult.score
            aiReasoning = aiResult.reasoning
        } ?: UserJobGroupEntity(
            user = preference.user,
            group = group,
            aiRelevanceScore = aiResult.score,
            aiReasoning = aiResult.reasoning,
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
                    null
                } else {
                    userId to chatClientFactory.createForUser(settings, AiUseCase.SCORING)
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
        data class Success(val jobs: List<JobEntity>, val stats: MatchingStats) : MatchResult

        data object Failure : MatchResult
    }

    companion object {
        private val MAX_REMATCH_PERIOD = Duration.ofDays(3)
        private const val COLD_ONLY_REASONING = "Cold filter match only — AI evaluation disabled"
    }
}
