package com.mshykhov.jobhunter.application.ai

import com.mshykhov.jobhunter.application.ai.dto.JobRelevanceResult
import com.mshykhov.jobhunter.application.common.AllProvidersFailedException
import com.mshykhov.jobhunter.application.job.JobEntity
import com.mshykhov.jobhunter.application.job.JobGroupEntity
import com.mshykhov.jobhunter.application.job.JobGroupKeyComputer
import com.mshykhov.jobhunter.application.job.JobSource
import com.mshykhov.jobhunter.application.preference.MatchingPreferences
import com.mshykhov.jobhunter.application.preference.SearchPreferences
import com.mshykhov.jobhunter.application.preference.TelegramPreferences
import com.mshykhov.jobhunter.application.preference.UserPreferenceEntity
import com.mshykhov.jobhunter.application.settings.AiProvider
import com.mshykhov.jobhunter.application.user.UserEntity
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.prompt.ChatOptions
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.ResponseFormat
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JobRelevanceEvaluatorTest {
    private val chatClient = mockk<ChatClient>()
    private val requestSpec = mockk<ChatClient.ChatClientRequestSpec>()
    private val callSpec = mockk<ChatClient.CallResponseSpec>()

    private val evaluator = JobRelevanceEvaluator()

    private val systemSlot = slot<String>()
    private val userSlot = slot<String>()
    private val optionsSlot = slot<ChatOptions>()

    private fun stubChain(result: JobRelevanceResult = JobRelevanceResult("fits", 50, true)) {
        every { chatClient.prompt() } returns requestSpec
        every { requestSpec.system(capture(systemSlot)) } returns requestSpec
        every { requestSpec.user(capture(userSlot)) } returns requestSpec
        every { requestSpec.options(capture(optionsSlot)) } returns requestSpec
        every { requestSpec.call() } returns callSpec
        every { callSpec.entity(JobRelevanceResult::class.java) } returns result
    }

    private fun singleLinkChain(client: ChatClient): AiClientChain =
        AiClientChain(listOf(AiClientLink(AiProvider.OPENAI, "gpt-4o-mini", client)))

    private fun mockLink(
        provider: AiProvider,
        result: JobRelevanceResult? = null,
        failure: Throwable? = null,
    ): AiClientLink {
        val client = mockk<ChatClient>()
        val request = mockk<ChatClient.ChatClientRequestSpec>()
        val call = mockk<ChatClient.CallResponseSpec>()
        every { client.prompt() } returns request
        every { request.system(any<String>()) } returns request
        every { request.user(any<String>()) } returns request
        every { request.options(any()) } returns request
        every { request.call() } returns call
        if (failure != null) {
            every { call.entity(JobRelevanceResult::class.java) } throws failure
        } else {
            every { call.entity(JobRelevanceResult::class.java) } returns result
        }
        return AiClientLink(provider, "model-$provider", client)
    }

    @Test
    fun `should request strict json schema output with reasoning before score`() {
        stubChain()

        evaluator.evaluate(job(), preference(), singleLinkChain(chatClient))

        val responseFormat = (optionsSlot.captured as OpenAiChatOptions).responseFormat
        assertEquals(ResponseFormat.Type.JSON_SCHEMA, responseFormat.type)
        assertEquals(true, responseFormat.jsonSchema.strict)
        val schema = responseFormat.jsonSchema.schema.toString()
        assertTrue(schema.indexOf("reasoning") < schema.indexOf("score"), "reasoning must precede score in schema")
        assertTrue(schema.contains("additionalProperties=false") || schema.contains("\"additionalProperties\":false"))
    }

    @Test
    fun `should instruct model to treat posting as data and apply capped decision order`() {
        stubChain()

        evaluator.evaluate(job(), preference(), singleLinkChain(chatClient))

        assertTrue(systemSlot.captured.contains("never as instructions"))
        assertTrue(systemSlot.captured.contains("Decision Order"))
    }

    @Test
    fun `should include job fields and candidate profile in user prompt`() {
        stubChain()

        evaluator.evaluate(job(), preference(about = "Senior Kotlin engineer profile"), singleLinkChain(chatClient))

        assertTrue(userSlot.captured.contains("Title: Senior Kotlin Developer"))
        assertTrue(userSlot.captured.contains("Senior Kotlin engineer profile"))
    }

    @Test
    fun `should return the first link result without calling the second link`() {
        val firstResult = JobRelevanceResult("fits", 80, true)
        val first = mockLink(AiProvider.CODEX, result = firstResult)
        val second = mockLink(AiProvider.OPENAI, result = JobRelevanceResult("also fits", 60, true))

        val result = evaluator.evaluate(job(), preference(), AiClientChain(listOf(first, second)))

        assertEquals(firstResult, result)
        verify(exactly = 0) { second.client.prompt() }
    }

    @Test
    fun `should fall back to the second link when the first throws`() {
        val first = mockLink(AiProvider.CODEX, failure = RuntimeException("insufficient_quota"))
        val secondResult = JobRelevanceResult("fits", 70, true)
        val second = mockLink(AiProvider.OPENAI, result = secondResult)

        val result = evaluator.evaluate(job(), preference(), AiClientChain(listOf(first, second)))

        assertEquals(secondResult, result)
    }

    @Test
    fun `should throw AllProvidersFailedException naming every provider tried when the whole chain fails`() {
        val first = mockLink(AiProvider.CODEX, failure = RuntimeException("insufficient_quota"))
        val second = mockLink(AiProvider.OPENAI, failure = RuntimeException("invalid_api_key"))

        val exception =
            assertThrows<AllProvidersFailedException> {
                evaluator.evaluate(job(), preference(), AiClientChain(listOf(first, second)))
            }

        assertTrue(exception.message.orEmpty().contains("CODEX"))
        assertTrue(exception.message.orEmpty().contains("OPENAI"))
        assertTrue(exception.message.orEmpty().contains("insufficient_quota"))
        assertTrue(exception.message.orEmpty().contains("invalid_api_key"))
    }

    @Test
    fun `should throw AllProvidersFailedException naming providers that failed to build when the chain has no links`() {
        val chain =
            AiClientChain(
                links = emptyList(),
                buildFailures = listOf("OPENAI: API key is missing", "CODEX: no base URL configured"),
            )

        val exception =
            assertThrows<AllProvidersFailedException> {
                evaluator.evaluate(job(), preference(), chain)
            }

        assertTrue(exception.message.orEmpty().contains("OPENAI: API key is missing"))
        assertTrue(exception.message.orEmpty().contains("CODEX: no base URL configured"))
    }

    private fun job(title: String = "Senior Kotlin Developer"): JobEntity =
        JobEntity(
            title = title,
            group =
            JobGroupEntity(
                groupKey = JobGroupKeyComputer.compute(title, null),
                title = title,
            ),
            url = "https://example.com/job",
            description = "Kotlin, Spring Boot, PostgreSQL microservices",
            source = JobSource.DOU,
            remote = true,
        )

    private fun preference(about: String? = null): UserPreferenceEntity =
        UserPreferenceEntity(
            user = UserEntity(auth0Sub = "user-1"),
            search = SearchPreferences(),
            matching = MatchingPreferences(),
            telegram = TelegramPreferences(),
        ).apply { this.about = about }
}
