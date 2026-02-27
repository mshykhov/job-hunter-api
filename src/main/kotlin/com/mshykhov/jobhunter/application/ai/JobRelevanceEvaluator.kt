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
        ## Job Posting
        - Title: ${job.title}
        - Company: ${job.company ?: "not specified"}
        - Description: ${job.description.take(3000)}
        - Location: ${job.location ?: "not specified"}
        - Remote: ${job.remote ?: "not specified — infer from title and description"}
        - Salary: ${job.salary ?: "not specified"}

        ## User Preferences
        - Target technologies: ${preference.categories.joinToString(", ").ifEmpty { "not specified" }}
        - Seniority levels: ${preference.seniorityLevels.joinToString(", ").ifEmpty { "not specified" }}
        - Desired skills/frameworks: ${preference.keywords.joinToString(", ").ifEmpty { "not specified" }}
        - Excluded keywords: ${preference.excludedKeywords.joinToString(", ").ifEmpty { "none" }}
        - Preferred locations: ${preference.locations.joinToString(", ").ifEmpty { "not specified" }}
        - Remote only: ${preference.remoteOnly}
        """.trimIndent()
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
    This field reflects whether the job offers remote work.

    If remote status is already provided in the job data, echo that value (true/false).

    If remote is "not specified", infer from title, description, and location:

    Set to true if any of these signals appear:
    - Keywords: "remote", "fully remote", "100% remote", "remote-first", "work from home", "WFH", "telecommute", "distributed team"
    - Phrases: "work from anywhere", "no office required", "remote-friendly", "home office allowed"
    - Hybrid arrangements: "hybrid", "flexible location", "partial remote" — these offer remote work, so count as true
    - Location lists countries/regions broadly, or says "worldwide", "global", "anywhere"

    Set to false if any of these signals appear and no remote signals contradict them:
    - Keywords: "on-site", "in-office", "office-based", "in-person", "no remote option"
    - Phrases: "must relocate to", "relocation required", "X days per week in office" (without remote option), "presence required"
    - Location is a specific city (e.g. "Kyiv", "Berlin", "New York") with zero mention of remote — a city-only location with no remote signal is a strong on-site indicator

    Set to null ONLY if:
    - There are genuinely contradictory signals that cannot be resolved
    - There is absolutely no location, no office/city context, and no remote-related keywords whatsoever

    Prefer a confident true or false. Most job postings have enough signals. Reserve null for truly unresolvable cases.

    ## Output
    Return a JSON object with:
    - score: integer 0–100
    - reasoning: 1–2 sentences explaining the score
    - inferredRemote: boolean or null — remote status determination (echoed from input or inferred)
    """.trimIndent()
