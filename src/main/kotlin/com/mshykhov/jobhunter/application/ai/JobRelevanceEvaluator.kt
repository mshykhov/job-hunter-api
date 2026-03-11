package com.mshykhov.jobhunter.application.ai

import com.mshykhov.jobhunter.application.ai.dto.JobRelevanceResult
import com.mshykhov.jobhunter.application.job.JobEntity
import com.mshykhov.jobhunter.application.preference.MatchingPreferences
import com.mshykhov.jobhunter.application.preference.SearchPreferences
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
        val components = buildScoringComponents(matching, search)

        return buildString {
            appendLine("## Job")
            appendLine("Title: ${job.title}")
            job.company?.let { appendLine("Company: $it") }
            appendLine("Description: ${job.description.take(DESCRIPTION_LIMIT)}")
            job.location?.let { appendLine("Location: $it") }
            appendLine("Remote: ${job.remote ?: "unknown — infer from description"}")
            job.salary?.let { appendLine("Salary: $it") }

            appendLine()
            appendLine("## Preferences")
            components.forEach { appendLine("${it.name}: ${it.value}") }

            appendLine()
            appendLine("## Weights")
            appendLine(components.joinToString(", ") { "${it.name}: ${it.weight}%" })

            if (!matching.customPrompt.isNullOrBlank()) {
                appendLine()
                appendLine("## Custom instructions")
                appendLine(matching.customPrompt)
            }
        }
    }

    companion object {
        private const val DESCRIPTION_LIMIT = 3000
    }
}

private data class ScoringComponent(val name: String, val value: String, val weight: Int)

private fun buildScoringComponents(
    matching: MatchingPreferences,
    search: SearchPreferences,
): List<ScoringComponent> {
    val raw = mutableListOf<ScoringComponent>()

    if (matching.keywords.isNotEmpty()) {
        raw.add(ScoringComponent("Keywords", matching.keywords.joinToString(", "), matching.weightKeywords))
    }
    if (matching.seniorityLevels.isNotEmpty()) {
        raw.add(ScoringComponent("Seniority", matching.seniorityLevels.joinToString(", "), matching.weightSeniority))
    }
    if (search.categories.isNotEmpty()) {
        raw.add(ScoringComponent("Categories", search.categories.joinToString(", "), matching.weightCategories))
    }

    if (raw.isEmpty()) {
        return listOf(ScoringComponent("General relevance", "evaluate overall fit", 100))
    }

    return redistributeWeights(raw)
}

private fun redistributeWeights(components: List<ScoringComponent>): List<ScoringComponent> {
    val totalWeight = components.sumOf { it.weight }
    if (totalWeight == 100) return components

    val normalized = components.map { it.copy(weight = it.weight * 100 / totalWeight) }
    val remainder = 100 - normalized.sumOf { it.weight }

    return normalized.mapIndexed { i, c ->
        if (i == 0) c.copy(weight = c.weight + remainder) else c
    }
}

private val SYSTEM_PROMPT =
    """
    You are a job relevance scoring engine.

    ## Weighted scoring (0–100)
    Score ONLY the components listed in the Weights section. Ignore unlisted components.
    The weights always sum to 100%.

    Component scoring rules:

    1. **Keywords** — overlap between job requirements and user's desired skills/keywords.
       Score proportionally to how many desired keywords appear in the job.
    2. **Seniority** — does the job match the user's experience level?
       Full points: exact match. Half: adjacent level (e.g. Senior vs Lead). Zero: 2+ levels off.
    3. **Categories** — does the job's PRIMARY technology stack match the user's target categories?
       Evaluate based on the MAIN technology the role requires, not "nice to have" or bonus skills.
       Full points: primary tech matches (e.g. user wants Java, job is Java).
       Half: closely related primary tech (e.g. user wants Kotlin, job is Java).
       Zero: different primary tech (e.g. user wants Java, job is C#/Python/.NET even if Java is mentioned as a bonus).

    Formula: score = sum(component_score * component_weight) / 100

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
