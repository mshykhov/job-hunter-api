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
        val basePrompt =
            """
            ## Job Posting
            - Title: ${job.title}
            - Company: ${job.company ?: "not specified"}
            - Description: ${job.description.take(3000)}
            - Location: ${job.location ?: "not specified"}
            - Remote: ${job.remote ?: "not specified — infer from title and description"}
            - Salary: ${job.salary ?: "not specified"}

            ## User Preferences
            - Target technologies: ${preference.search.categories.joinToString(", ").ifEmpty { "not specified" }}
            - Seniority levels: ${preference.matching.seniorityLevels.joinToString(", ").ifEmpty { "not specified" }}
            - Desired skills/frameworks: ${preference.matching.keywords.joinToString(", ").ifEmpty { "not specified" }}
            - Excluded keywords: ${preference.matching.excludedKeywords.joinToString(", ").ifEmpty { "none" }}
            - Preferred locations: ${preference.search.locations.joinToString(", ").ifEmpty { "not specified" }}
            - Remote only: ${preference.search.remoteOnly}
            """.trimIndent()

        val customPrompt = preference.matching.customPrompt
        if (customPrompt.isNullOrBlank()) return basePrompt

        return basePrompt +
            "\n\n" +
            """
            ## Additional user instructions
            $customPrompt
            """.trimIndent()
    }
}

private val SYSTEM_PROMPT =
    """
    You are a job relevance scoring engine. Given a job posting and user preferences, return a relevance score and reasoning.

    ## Scoring rules (0–100)
    - 80–100: Strong match — core technology AND seniority match, no excluded keywords
    - 60–79: Partial match — related technology or adjacent seniority level
    - 40–59: Weak match — some keyword overlap but significant mismatches
    - 0–39: Poor match — wrong domain, wrong seniority, or contains excluded keywords

    ## Evaluation criteria (in priority order)
    1. Technology/category match — does the job require the user's target technologies?
    2. Seniority level — does the job match the user's experience level?
    3. Excluded keywords — if ANY excluded keyword appears in the job, score ≤ 30
    4. Skill/framework overlap — bonus for matching desired skills

    ## inferredRemote field
    Always return true or false. Never return null. This field must always have a definitive value.

    If remote status is already provided in the job data, echo that value.

    If remote is "not specified", infer from title, description, and location:

    Set to true if any of these signals appear:
    - Keywords: "remote", "fully remote", "100% remote", "remote-first", "work from home", "WFH", "telecommute", "distributed team"
    - Phrases: "work from anywhere", "no office required", "remote-friendly", "home office allowed"
    - Hybrid arrangements: "hybrid", "flexible location", "partial remote" — they offer remote work, count as true
    - Location lists countries/regions broadly, or says "worldwide", "global", "anywhere"

    Set to false if any of these signals appear and no remote signals contradict them:
    - Keywords: "on-site", "in-office", "office-based", "in-person", "no remote option"
    - Phrases: "must relocate to", "relocation required", "X days per week in office" (without remote option), "presence required"
    - Location is a specific city (e.g. "Kyiv", "Berlin", "New York") with zero remote mention — city-only location with no remote signal is a strong on-site indicator

    If signals are completely absent, default to false (assume on-site).

    ## Output
    Return a JSON object with:
    - score: integer 0–100
    - reasoning: 1–2 sentences explaining the score
    - inferredRemote: boolean (never null) — true if remote/hybrid, false if on-site
    """.trimIndent()
