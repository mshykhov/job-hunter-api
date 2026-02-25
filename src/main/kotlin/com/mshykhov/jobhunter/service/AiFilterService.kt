package com.mshykhov.jobhunter.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.mshykhov.jobhunter.config.AiProperties
import com.mshykhov.jobhunter.persistence.model.JobEntity
import com.mshykhov.jobhunter.persistence.model.UserPreferenceEntity
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class AiFilterService(
    private val claudeClient: ClaudeClient,
    private val aiProperties: AiProperties,
    private val objectMapper: ObjectMapper,
) {
    fun evaluate(
        job: JobEntity,
        preference: UserPreferenceEntity,
    ): AiFilterResult? =
        try {
            val prompt = buildPrompt(job, preference)
            val response = claudeClient.sendMessage(prompt, aiProperties.filter.maxTokens) ?: return null
            parseResponse(response)
        } catch (e: Exception) {
            logger.error(e) { "AI filter failed for job '${job.title}'" }
            null
        }

    private fun buildPrompt(
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
        |Min Salary: ${preference.minSalary ?: "Not specified"}
        |Remote Only: ${preference.remoteOnly}
        |
        |Evaluate how relevant this job is to the user's preferences.
        |Consider: seniority level match, category/domain match, keyword relevance, salary fit, remote preference.
        |
        |Return ONLY a valid JSON object with no additional text:
        |{"score": <0-100>, "reasoning": "<brief explanation in 1-2 sentences>"}
        """.trimMargin()

    private fun parseResponse(response: String): AiFilterResult? {
        val json = extractJson(response) ?: return null
        val tree = objectMapper.readTree(json)
        val score = tree.get("score")?.asInt() ?: return null
        val reasoning = tree.get("reasoning")?.asText() ?: return null
        return AiFilterResult(score = score, reasoning = reasoning)
    }

    private fun extractJson(text: String): String? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end < 0 || end <= start) return null
        return text.substring(start, end + 1)
    }
}
