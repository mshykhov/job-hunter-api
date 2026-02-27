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
    4. Remote preference — if user wants remote only and job is clearly on-site, reduce score by 20
    5. Skill/framework overlap — bonus for matching desired skills

    ## Remote field
    If remote is "not specified", infer from title and description. Look for: "remote", "work from home", "distributed", "anywhere". If unclear, treat as neutral (do not penalize).

    ## Output
    Return a JSON object with:
    - score: integer 0–100
    - reasoning: 1–2 sentences explaining the score
    """.trimIndent()
