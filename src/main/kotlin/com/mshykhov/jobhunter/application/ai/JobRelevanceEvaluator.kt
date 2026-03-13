package com.mshykhov.jobhunter.application.ai

import com.mshykhov.jobhunter.application.ai.dto.JobRelevanceResult
import com.mshykhov.jobhunter.application.job.JobEntity
import com.mshykhov.jobhunter.application.preference.UserPreferenceEntity
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service

@Service
class JobRelevanceEvaluator {
    fun evaluate(
        job: JobEntity,
        preference: UserPreferenceEntity,
        chatClient: ChatClient,
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
    ): String {
        val search = preference.search

        return buildString {
            appendLine("## Job")
            appendLine("Title: ${job.title}")
            job.company?.let { appendLine("Company: $it") }
            appendLine("Description: ${job.description.take(DESCRIPTION_LIMIT)}")
            job.location?.let { appendLine("Location: $it") }
            appendLine("Remote: ${job.remote ?: "unknown — infer from description"}")
            job.salary?.let { appendLine("Salary: $it") }

            if (search.categories.isNotEmpty()) {
                appendLine()
                appendLine("## Preferences")
                appendLine("Categories: ${search.categories.joinToString(", ")}")
            }

            if (!preference.matching.customPrompt.isNullOrBlank()) {
                appendLine()
                appendLine("## Custom instructions")
                appendLine(preference.matching.customPrompt)
            }
        }
    }

    companion object {
        private const val DESCRIPTION_LIMIT = 3000
    }
}

private val SYSTEM_PROMPT =
    """
    You are a job relevance scoring engine.

    ## Scoring (0–100)
    Evaluate how well the job matches the user's preferences.

    If categories are provided, evaluate whether the job's PRIMARY technology stack matches.
    Full points: primary tech matches (e.g. user wants Java, job is Java).
    Half: closely related primary tech (e.g. user wants Kotlin, job is Java).
    Zero: different primary tech (e.g. user wants Java, job is C#/Python/.NET even if Java is mentioned as a bonus).

    If custom instructions are provided, follow them for scoring adjustments.

    If no preferences are provided, evaluate overall job quality and relevance.

    ## inferredRemote
    Always return true or false. Never null.

    If remote status is provided in job data, echo that value.

    If remote is "unknown", infer from description:
    - true ONLY for fully remote positions: "remote", "fully remote", "100% remote", "remote-first", "work from anywhere", "distributed team", "work from home"
    - false for hybrid, partial remote, flexible office, or any arrangement requiring office presence
    - false for on-site: "on-site", "in-office", "office-based", "relocation required"
    - false if no remote signals found (assume on-site)

    IMPORTANT: hybrid and partial remote count as FALSE. Only pure fully-remote = true.

    ## Output
    JSON: { "score": 0-100, "reasoning": "1-2 sentences", "inferredRemote": true/false }
    """.trimIndent()
