package com.mshykhov.jobhunter.application.matching

import com.fasterxml.jackson.databind.ObjectMapper
import com.mshykhov.jobhunter.application.job.JobEntity
import com.mshykhov.jobhunter.application.job.JobFacade
import com.mshykhov.jobhunter.application.preference.UserPreferenceEntity
import com.mshykhov.jobhunter.application.preference.UserPreferenceFacade
import com.mshykhov.jobhunter.application.userjob.UserJobEntity
import com.mshykhov.jobhunter.application.userjob.UserJobFacade
import com.mshykhov.jobhunter.infrastructure.ai.AiProperties
import com.mshykhov.jobhunter.infrastructure.ai.ClaudeClient
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant

private val logger = KotlinLogging.logger {}

private data class AiFilterResult(
    val score: Int,
    val reasoning: String,
)

@Service
class JobMatchingService(
    private val jobFacade: JobFacade,
    private val userPreferenceFacade: UserPreferenceFacade,
    private val userJobFacade: UserJobFacade,
    private val claudeClient: ClaudeClient,
    private val aiProperties: AiProperties,
    private val objectMapper: ObjectMapper,
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

    private fun matchJobToUsers(
        job: JobEntity,
        preferences: List<UserPreferenceEntity>,
    ) {
        val coldMatches = preferences.filter { coldFilterMatches(job, it) }
        if (coldMatches.isEmpty()) return

        val userJobs =
            coldMatches.mapNotNull { preference ->
                val aiResult = evaluateWithAi(job, preference)
                if (aiResult != null && aiResult.score < aiProperties.filter.minScore) {
                    logger.debug {
                        "Job '${job.title}' filtered out by AI for user ${preference.user.id} (score: ${aiResult.score})"
                    }
                    return@mapNotNull null
                }
                UserJobEntity(
                    user = preference.user,
                    job = job,
                    aiRelevanceScore = aiResult?.score,
                    aiReasoning = aiResult?.reasoning,
                )
            }

        if (userJobs.isNotEmpty()) {
            userJobFacade.saveAll(userJobs)
            logger.debug { "Job '${job.title}' matched ${userJobs.size} users" }
        }
    }

    private fun markAllMatched(jobs: List<JobEntity>) {
        val now = Instant.now(clock)
        jobs.forEach { it.matchedAt = now }
        jobFacade.saveAll(jobs)
    }

    // --- Cold filter ---

    private fun coldFilterMatches(
        job: JobEntity,
        preference: UserPreferenceEntity,
    ): Boolean =
        isSourceEnabled(job, preference) &&
            isRemoteMatch(job, preference) &&
            hasNoExcludedKeywords(job, preference) &&
            matchesCategories(job, preference)

    private fun isSourceEnabled(
        job: JobEntity,
        preference: UserPreferenceEntity,
    ): Boolean {
        if (preference.enabledSources.isEmpty()) return true
        return job.source in preference.enabledSources
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
        val searchText = "${job.title} ${job.description}".lowercase()
        return preference.excludedKeywords.none { keyword ->
            searchText.contains(keyword.lowercase())
        }
    }

    private fun matchesCategories(
        job: JobEntity,
        preference: UserPreferenceEntity,
    ): Boolean {
        if (preference.categories.isEmpty()) return true
        val searchText = "${job.title} ${job.description}".lowercase()
        return preference.categories.any { category ->
            searchText.contains(category.lowercase())
        }
    }

    // --- AI filter ---

    private fun evaluateWithAi(
        job: JobEntity,
        preference: UserPreferenceEntity,
    ): AiFilterResult? {
        if (!aiProperties.enabled) return null
        return try {
            val prompt = buildAiPrompt(job, preference)
            val response = claudeClient.sendMessage(prompt, aiProperties.filter.maxTokens) ?: return null
            parseAiResponse(response)
        } catch (e: Exception) {
            logger.error(e) { "AI filter failed for job '${job.title}'" }
            null
        }
    }

    private fun buildAiPrompt(
        job: JobEntity,
        preference: UserPreferenceEntity,
    ): String =
        """
        |You are a job relevance evaluator. Analyze the job posting against the user's preferences.
        |
        |JOB POSTING:
        |Title: ${job.title}
        |Company: ${job.company ?: "N/A"}
        |Description: ${job.description.take(2000)}
        |Location: ${job.location ?: "N/A"}
        |Remote: ${job.remote}
        |Salary: ${job.salary ?: "N/A"}
        |
        |USER PREFERENCES:
        |Categories: ${preference.categories.joinToString(", ").ifEmpty { "Any" }}
        |Seniority Levels: ${preference.seniorityLevels.joinToString(", ").ifEmpty { "Any" }}
        |Keywords: ${preference.keywords.joinToString(", ").ifEmpty { "Any" }}
        |Remote Only: ${preference.remoteOnly}
        |
        |Evaluate how relevant this job is to the user's preferences.
        |Consider: seniority level match, technology/category match, keyword relevance, remote preference.
        |
        |Return ONLY a valid JSON object with no additional text:
        |{"score": <0-100>, "reasoning": "<brief explanation in 1-2 sentences>"}
        """.trimMargin()

    private fun parseAiResponse(response: String): AiFilterResult? {
        val json = ClaudeClient.extractJson(response) ?: return null
        val tree = objectMapper.readTree(json)
        val score = tree.get("score")?.asInt() ?: return null
        val reasoning = tree.get("reasoning")?.asText() ?: return null
        return AiFilterResult(score = score, reasoning = reasoning)
    }
}
