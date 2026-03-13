package com.mshykhov.jobhunter.application.ai

import com.mshykhov.jobhunter.application.common.ServiceUnavailableException
import com.mshykhov.jobhunter.application.job.JobEntity
import com.mshykhov.jobhunter.application.outreach.OutreachSourceConfig
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service
import kotlin.time.measureTimedValue

@Service
class OutreachGenerator {
    private val log = LoggerFactory.getLogger(javaClass)

    fun generateCoverLetter(
        job: JobEntity,
        about: String?,
        sourceConfig: OutreachSourceConfig?,
        defaultPrompt: String?,
        chatClient: ChatClient,
    ): String {
        val prompt = resolvePrompt(sourceConfig?.coverLetterPrompt, defaultPrompt, DEFAULT_COVER_LETTER_PROMPT)
        return generate(job, about, prompt, chatClient, "cover letter")
    }

    fun generateRecruiterMessage(
        job: JobEntity,
        about: String?,
        sourceConfig: OutreachSourceConfig?,
        defaultPrompt: String?,
        chatClient: ChatClient,
    ): String {
        val prompt = resolvePrompt(sourceConfig?.recruiterMessagePrompt, defaultPrompt, DEFAULT_RECRUITER_MESSAGE_PROMPT)
        return generate(job, about, prompt, chatClient, "recruiter message")
    }

    private fun generate(
        job: JobEntity,
        about: String?,
        systemPrompt: String,
        chatClient: ChatClient,
        type: String,
    ): String {
        val userPrompt = buildUserPrompt(job, about)
        val (result, duration) =
            measureTimedValue {
                chatClient
                    .prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content()
                    ?: throw ServiceUnavailableException("AI returned empty response")
            }
        log.info(
            "Generated {} for job [{}] at [{}] in {}ms ({} chars)",
            type,
            job.title,
            job.company,
            duration.inWholeMilliseconds,
            result.length,
        )
        return result.trim()
    }

    private fun buildUserPrompt(
        job: JobEntity,
        about: String?,
    ): String =
        buildString {
            appendLine("## Job")
            appendLine("Title: ${job.title}")
            job.company?.let { appendLine("Company: $it") }
            appendLine("Description: ${job.description.take(DESCRIPTION_LIMIT)}")

            if (!about.isNullOrBlank()) {
                appendLine()
                appendLine("## Candidate Background")
                appendLine(about)
            }
        }

    companion object {
        private const val DESCRIPTION_LIMIT = 3000

        val DEFAULT_COVER_LETTER_PROMPT =
            """
            Write a 2-3 sentence cover letter for this job application.
            Be concise, professional, and specific to the role.
            Mention relevant skills from the candidate's background.
            Output plain text only, no greeting or sign-off.
            """.trimIndent()

        val DEFAULT_RECRUITER_MESSAGE_PROMPT =
            """
            Write a 2-3 sentence message to a recruiter about this job.
            Tone: friendly, professional, brief.
            Mention you've applied and express genuine interest.
            Output plain text only.
            """.trimIndent()

        fun resolvePrompt(
            sourceSpecific: String?,
            userDefault: String?,
            systemDefault: String,
        ): String = sourceSpecific ?: userDefault ?: systemDefault
    }
}
