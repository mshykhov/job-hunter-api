package com.mshykhov.jobhunter.application.ai

import com.mshykhov.jobhunter.application.common.AiNotConfiguredException
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertNotNull

class ChatClientFactoryTest {
    private val factory = ChatClientFactory()

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
}
