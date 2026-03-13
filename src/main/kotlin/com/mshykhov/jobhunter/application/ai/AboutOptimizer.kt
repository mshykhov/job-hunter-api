package com.mshykhov.jobhunter.application.ai

import com.mshykhov.jobhunter.application.common.ServiceUnavailableException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class AboutOptimizer {
    fun optimize(
        rawAbout: String,
        chatClient: ChatClient,
    ): String {
        logger.info { "Optimizing about section, input length: ${rawAbout.length}" }
        return try {
            val result =
                chatClient
                    .prompt()
                    .system(SYSTEM_PROMPT)
                    .user(rawAbout)
                    .call()
                    .content()
                    ?.trim()
                    ?: throw ServiceUnavailableException("AI returned empty response")
            logger.info { "About optimized: ${rawAbout.length} → ${result.length} chars" }
            result
        } catch (e: Exception) {
            logger.error(e) { "About optimization failed" }
            throw ServiceUnavailableException("About optimization failed: ${e.message}")
        }
    }
}

private val SYSTEM_PROMPT =
    """
    You are a profile optimizer for a job matching system. Clean and restructure the candidate's raw profile text
    to maximize AI matching accuracy against job descriptions.

    ## Rules
    - REMOVE: emojis, icons, decorative characters, contact info (email, phone, telegram, links), section headers/dividers
    - KEEP: all technical skills, frameworks, tools, years of experience, job titles, company names, achievements, metrics
    - KEEP: domain experience (fintech, e-commerce, etc.), soft skills, team size, responsibilities
    - DO NOT invent or add information that is not in the original text
    - DO NOT add your own commentary or meta-text

    ## Output Format
    Write a concise, plain-text professional profile in this structure:

    [Role] with [X]+ years of experience. [1-2 sentence summary of focus areas and strengths].

    Skills: [comma-separated list of all technical skills and tools mentioned]

    Experience:
    - [Company/Context] ([dates]): [Role]. [Key responsibilities and achievements in 1-2 sentences]
    - [repeat for each position]

    ## Length
    Keep the output under 2500 characters. Prioritize recent and relevant experience.
    Output plain text only — no markdown, no headers with #, no bullet symbols other than -.
    """.trimIndent()
