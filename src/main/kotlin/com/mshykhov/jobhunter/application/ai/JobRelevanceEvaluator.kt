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
        val matching = preference.matching
        val search = preference.search

        val prompt =
            buildString {
                appendLine("## Job")
                appendLine("Title: ${job.title}")
                job.company?.let { appendLine("Company: $it") }
                appendLine("Description: ${job.description.take(DESCRIPTION_LIMIT)}")
                job.location?.let { appendLine("Location: $it") }
                appendLine("Remote: ${job.remote ?: "unknown — infer from description"}")
                job.salary?.let { appendLine("Salary: $it") }

                appendLine()
                appendLine("## Preferences")
                appendLine("Categories: ${search.categories.joinToString(", ").ifEmpty { "any" }}")
                appendLine("Seniority: ${matching.seniorityLevels.joinToString(", ").ifEmpty { "any" }}")
                appendLine("Keywords: ${matching.keywords.joinToString(", ").ifEmpty { "any" }}")

                appendLine()
                appendLine("## Weights")
                appendLine(
                    "Keywords: ${matching.weightKeywords}%, Seniority: ${matching.weightSeniority}%, " +
                        "Categories: ${matching.weightCategories}%",
                )

                if (!matching.customPrompt.isNullOrBlank()) {
                    appendLine()
                    appendLine("## Custom instructions")
                    appendLine(matching.customPrompt)
                }
            }

        return prompt
    }

    companion object {
        private const val DESCRIPTION_LIMIT = 3000
    }
}

private val SYSTEM_PROMPT =
    """
    You are a job relevance scoring engine.

    ## Weighted scoring (0–100)
    Score each component separately, then compute weighted total.
    The user provides weights (summing to 100%) for these components:

    1. **Keywords** — overlap between job requirements and user's desired skills/keywords.
       Score proportionally to how many desired keywords appear in the job.
    2. **Seniority** — does the job match the user's experience level?
       Full points: exact match. Half: adjacent level (e.g. Senior vs Lead). Zero: 2+ levels off.
    3. **Categories** — does the job match the user's target technology categories?
       Full points: core category match. Half: related/adjacent tech. Zero: different domain.

    Formula: score = (keywords_score * keywords_weight + seniority_score * seniority_weight + categories_score * categories_weight) / 100

    Each component_score is 0–100. Final score is 0–100.

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
