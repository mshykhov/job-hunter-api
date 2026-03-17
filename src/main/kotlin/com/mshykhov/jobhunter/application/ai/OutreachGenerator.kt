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
            You write cover letter text for job applications.

            <rules>
            - Write 3-4 sentences, one paragraph
            - First person, natural professional tone
            - Sentence 1: name the specific role and connect your strongest matching skill to their key requirement
            - Sentence 2-3: briefly explain why you fit — reference specific technologies or experience from both the job and your background
            - Last sentence: express interest in contributing to their team/product
            </rules>

            <forbidden>
            - "I am writing to express my interest"
            - "I am passionate about" / "I am excited to"
            - "I believe I would be a great fit"
            - "With my X years of experience"
            - "I am confident that"
            - Any greeting (Dear, Hello) or sign-off (Sincerely, Best regards)
            </forbidden>

            Output ONLY the cover letter text. Plain text, no markdown.
            """.trimIndent()

        val DEFAULT_RECRUITER_MESSAGE_PROMPT =
            """
            You write short messages from a candidate to a recruiter about a specific role.

            <rules>
            - Write 2-3 sentences
            - Conversational, confident, peer-to-peer tone
            - Sentence 1: mention the role and one specific thing that caught your attention (tech stack, product, company mission)
            - Sentence 2: your relevant experience — name specific technologies, not generic claims
            - Last sentence: suggest a brief chat or ask if the role is still open
            </rules>

            <forbidden>
            - "I am reaching out regarding" / "I came across your posting"
            - "I am passionate about" / "I am excited about"
            - "I believe my skills align perfectly"
            - "I would love the opportunity"
            - Any greeting or sign-off
            - Exclamation marks
            </forbidden>

            Output ONLY the message text. Plain text, no markdown.
            """.trimIndent()

        fun resolvePrompt(
            sourceSpecific: String?,
            userDefault: String?,
            systemDefault: String,
        ): String = sourceSpecific ?: userDefault ?: systemDefault
    }
}
