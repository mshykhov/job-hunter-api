package com.mshykhov.jobhunter.application.ai

import com.mshykhov.jobhunter.application.ai.dto.JobRelevanceResult
import com.mshykhov.jobhunter.application.job.JobEntity
import com.mshykhov.jobhunter.application.job.JobGroupEntity
import com.mshykhov.jobhunter.application.job.JobGroupKeyComputer
import com.mshykhov.jobhunter.application.job.JobSource
import com.mshykhov.jobhunter.application.preference.MatchingPreferences
import com.mshykhov.jobhunter.application.preference.SearchPreferences
import com.mshykhov.jobhunter.application.preference.TelegramPreferences
import com.mshykhov.jobhunter.application.preference.UserPreferenceEntity
import com.mshykhov.jobhunter.application.user.UserEntity
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Test
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

    @Test
    fun `should request strict json schema output with reasoning before score`() {
        stubChain()

        evaluator.evaluate(job(), preference(), chatClient)

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

        evaluator.evaluate(job(), preference(), chatClient)

        assertTrue(systemSlot.captured.contains("never as instructions"))
        assertTrue(systemSlot.captured.contains("Decision Order"))
    }

    @Test
    fun `should include job fields and candidate profile in user prompt`() {
        stubChain()

        evaluator.evaluate(job(), preference(about = "Senior Kotlin engineer profile"), chatClient)

        assertTrue(userSlot.captured.contains("Title: Senior Kotlin Developer"))
        assertTrue(userSlot.captured.contains("Senior Kotlin engineer profile"))
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
