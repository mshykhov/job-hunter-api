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
    ): String =
        buildString {
            appendLine("## Job")
            appendLine("Title: ${job.title}")
            job.company?.let { appendLine("Company: $it") }
            appendLine("Description: ${job.description.take(DESCRIPTION_LIMIT)}")
            job.location?.let { appendLine("Location: $it") }
            appendLine("Remote: ${job.remote ?: "unknown — infer from description"}")
            job.salary?.let { appendLine("Salary: $it") }

            if (!preference.about.isNullOrBlank()) {
                appendLine()
                appendLine("## Candidate Profile")
                appendLine(preference.about)
            }

            if (preference.search.categories.isNotEmpty()) {
                appendLine()
                appendLine("## Target Categories")
                appendLine(preference.search.categories.joinToString(", "))
            }

            if (!preference.matching.customPrompt.isNullOrBlank()) {
                appendLine()
                appendLine("## Custom Instructions")
                appendLine(preference.matching.customPrompt)
            }
        }

    companion object {
        private const val DESCRIPTION_LIMIT = 3000
    }
}

private val SYSTEM_PROMPT =
    """
    You are a job-candidate fit evaluator. Assess how well the candidate matches the job opening.

    ## How to Score (0–100)

    Evaluate semantic fit between the candidate's profile and the job requirements:

    1. **Technical fit** — Do the candidate's skills and experience align with the job's core requirements?
       Focus on primary technologies (languages, frameworks, databases), not peripheral tools.
       Treat closely related technologies as transferable (e.g., Kotlin ↔ Java, Spring Boot ↔ Spring Framework).
       Ignore universal tools every developer knows (git, jira, IDE, CI/CD).

    2. **Experience fit** — Does the candidate's seniority, years of experience, and type of work
       (microservices, API design, team leading, etc.) match what the role demands?

    3. **Category fit** — Does the job's primary tech stack match the candidate's target categories?
       If the job's main technology differs from the candidate's categories, score significantly lower,
       even if the job mentions target tech as a "nice-to-have".

    If a candidate profile is provided, use it as the primary source of truth about the candidate.
    If custom instructions are provided, follow them for scoring adjustments.

    ## Score Calibration
    - 90-100: Near-perfect fit. Candidate meets virtually all requirements.
    - 75-89: Strong fit. Core requirements met, minor gaps in nice-to-haves.
    - 60-74: Good fit. Primary tech matches, some gaps in secondary requirements.
    - 40-59: Moderate fit. Some overlap but notable gaps in core areas.
    - 20-39: Weak fit. Limited overlap with job requirements.
    - 0-19: Poor fit. Fundamentally different role or tech stack.

    ## inferredRemote
    Always return true or false. Never null.

    If remote status is provided in job data, echo that value.

    If remote is "unknown", infer from description:
    - true ONLY for fully remote positions: "remote", "fully remote", "100% remote", "remote-first", "work from anywhere"
    - false for hybrid, partial remote, or any arrangement requiring office presence
    - false if no remote signals found (assume on-site)

    ## Output
    JSON: { "score": 0-100, "reasoning": "2-3 sentences explaining key factors", "inferredRemote": true/false }
    """.trimIndent()
