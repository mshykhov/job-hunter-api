package com.mshykhov.jobhunter.application.ai

import com.mshykhov.jobhunter.application.ai.dto.JobRelevanceResult
import com.mshykhov.jobhunter.application.job.JobEntity
import com.mshykhov.jobhunter.application.preference.UserPreferenceEntity
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service

@Service
class JobRelevanceEvaluator(
    private val chatClient: ChatClient,
) {
    fun evaluate(
        job: JobEntity,
        preference: UserPreferenceEntity,
    ): JobRelevanceResult =
        chatClient
            .prompt()
            .system(SYSTEM_PROMPT)
            .user(buildUserPrompt(job, preference))
            .call()
            .entity(JobRelevanceResult::class.java)!!

    private fun buildUserPrompt(
        job: JobEntity,
        preference: UserPreferenceEntity,
    ): String =
        """
        JOB POSTING:
        Title: ${job.title}
        Company: ${job.company ?: "N/A"}
        Description: ${job.description.take(2000)}
        Location: ${job.location ?: "N/A"}
        Remote: ${job.remote}
        Salary: ${job.salary ?: "N/A"}

        USER PREFERENCES:
        Categories: ${preference.categories.joinToString(", ").ifEmpty { "Any" }}
        Seniority Levels: ${preference.seniorityLevels.joinToString(", ").ifEmpty { "Any" }}
        Keywords: ${preference.keywords.joinToString(", ").ifEmpty { "Any" }}
        Remote Only: ${preference.remoteOnly}
        """.trimIndent()
}

private val SYSTEM_PROMPT =
    """
    You are a job relevance evaluator. Analyze the job posting against the user's preferences.
    Evaluate how relevant this job is to the user's preferences.
    Consider: seniority level match, technology/category match, keyword relevance, remote preference.
    Return a score from 0 to 100 and a brief reasoning in 1-2 sentences.
    """.trimIndent()
