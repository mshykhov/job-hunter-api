package com.mshykhov.jobhunter.application.ai

import com.mshykhov.jobhunter.application.common.AiNotConfiguredException
import com.mshykhov.jobhunter.application.settings.AiProvider
import com.mshykhov.jobhunter.infrastructure.ai.AiProviderProperties
import com.mshykhov.jobhunter.support.TestFixtures
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private const val CODEX_BASE_URL = "http://cli-proxy-api-prd.cli-proxy-api-prd.svc:8317/v1"

class ChatClientFactoryTest {
    private val factory = ChatClientFactory(AiProviderProperties())

    @Nested
    inner class CreateForUser {
        @Test
        fun `should throw AiNotConfiguredException when API key is blank`() {
            val settings = mockk<UserAiSettingsEntity>()
            every { settings.apiKey } returns ""

            assertThrows<AiNotConfiguredException> {
                factory.createForUser(settings)
            }
        }

        @Test
        fun `should create client with default SCORING use case`() {
            val settings = mockk<UserAiSettingsEntity>()
            every { settings.apiKey } returns "test-key"
            every { settings.modelId } returns "gpt-4o-mini"

            val client = factory.createForUser(settings)

            assertNotNull(client)
        }

        @ParameterizedTest
        @ValueSource(strings = ["gpt-5-nano", "gpt-5", "o1-mini", "o3-mini", "o4-mini"])
        fun `should create client for reasoning models without error`(modelId: String) {
            val settings = mockk<UserAiSettingsEntity>()
            every { settings.apiKey } returns "test-key"
            every { settings.modelId } returns modelId

            val client = factory.createForUser(settings, AiUseCase.OUTREACH)

            assertNotNull(client)
        }

        @ParameterizedTest
        @ValueSource(strings = ["gpt-4o-mini", "gpt-4o", "claude-haiku", "claude-sonnet"])
        fun `should create client for standard models without error`(modelId: String) {
            val settings = mockk<UserAiSettingsEntity>()
            every { settings.apiKey } returns "test-key"
            every { settings.modelId } returns modelId

            val client = factory.createForUser(settings, AiUseCase.SCORING)

            assertNotNull(client)
        }
    }

    @Nested
    inner class CreateForProvider {
        @Test
        fun `should build client for a CODEX row without an API key`() {
            val factory = ChatClientFactory(AiProviderProperties(codexBaseUrl = CODEX_BASE_URL))
            val provider =
                TestFixtures.userAiProviderEntity(provider = AiProvider.CODEX, apiKey = "", modelId = "gpt-5.6-luna")

            val client = factory.createForProvider(provider)

            assertNotNull(client)
        }

        @Test
        fun `should throw AiNotConfiguredException when CODEX has no configured base url`() {
            val provider =
                TestFixtures.userAiProviderEntity(provider = AiProvider.CODEX, apiKey = "", modelId = "gpt-5.6-luna")

            val exception = assertThrows<AiNotConfiguredException> { factory.createForProvider(provider) }

            assertTrue(exception.message!!.contains("AI_CODEX_BASE_URL"))
        }

        @Test
        fun `should throw AiNotConfiguredException when OPENAI row has a blank key`() {
            val provider =
                TestFixtures.userAiProviderEntity(provider = AiProvider.OPENAI, apiKey = "", modelId = "gpt-4o-mini")

            assertThrows<AiNotConfiguredException> { factory.createForProvider(provider) }
        }

        @Test
        fun `should treat dotted gpt-5 point releases as reasoning models`() {
            assertTrue(ChatClientFactory.isReasoningModel("gpt-5.6-luna"))
        }

        @Test
        fun `should not treat gpt-4 models as reasoning models`() {
            assertFalse(ChatClientFactory.isReasoningModel("gpt-4o-mini"))
        }
    }

    @Nested
    inner class CreateChain {
        private val factory = ChatClientFactory(AiProviderProperties(codexBaseUrl = CODEX_BASE_URL))

        @Test
        fun `should exclude disabled rows from the chain`() {
            val enabled =
                TestFixtures.userAiProviderEntity(priority = 1, provider = AiProvider.OPENAI, apiKey = "sk-test", enabled = true)
            val disabled =
                TestFixtures.userAiProviderEntity(priority = 2, provider = AiProvider.CODEX, apiKey = "", enabled = false)

            val chain = factory.createChain(listOf(enabled, disabled))

            assertEquals(listOf(AiProvider.OPENAI), chain.links.map { it.provider })
        }

        @Test
        fun `should preserve priority order regardless of input order`() {
            val second = TestFixtures.userAiProviderEntity(priority = 2, provider = AiProvider.CODEX, apiKey = "")
            val first = TestFixtures.userAiProviderEntity(priority = 1, provider = AiProvider.OPENAI, apiKey = "sk-test")

            val chain = factory.createChain(listOf(second, first))

            assertEquals(listOf(AiProvider.OPENAI, AiProvider.CODEX), chain.links.map { it.provider })
        }

        @Test
        fun `should skip a row that fails to build and keep the rest of the chain`() {
            val broken = TestFixtures.userAiProviderEntity(priority = 1, provider = AiProvider.OPENAI, apiKey = "")
            val working = TestFixtures.userAiProviderEntity(priority = 2, provider = AiProvider.CODEX, apiKey = "")

            val chain = factory.createChain(listOf(broken, working))

            assertEquals(listOf(AiProvider.CODEX), chain.links.map { it.provider })
        }
    }
}
