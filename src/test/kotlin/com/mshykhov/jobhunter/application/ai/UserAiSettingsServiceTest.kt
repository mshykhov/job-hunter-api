package com.mshykhov.jobhunter.application.ai

import com.mshykhov.jobhunter.api.rest.settings.dto.SaveAiSettingsRequest
import com.mshykhov.jobhunter.application.common.AiNotConfiguredException
import com.mshykhov.jobhunter.application.user.UserFacade
import com.mshykhov.jobhunter.support.TestFixtures
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UserAiSettingsServiceTest {
    private val userFacade = mockk<UserFacade>()
    private val userAiSettingsFacade = mockk<UserAiSettingsFacade>()
    private val service = UserAiSettingsService(userFacade, userAiSettingsFacade)

    private val auth0Sub = "auth0|test-user"
    private val user = TestFixtures.userEntity(auth0Sub = auth0Sub)

    @Nested
    inner class Get {
        @Test
        fun `should return AI settings response for existing user`() {
            val settings = UserAiSettingsEntity(user = user, apiKey = "sk-ant-api03-secret-key", modelId = "claude-sonnet-4-20250514")
            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userAiSettingsFacade.findByUserId(user.id) } returns settings

            val result = service.get(auth0Sub)

            assertEquals("claude-sonnet-4-20250514", result.modelId)
        }

        @Test
        fun `should mask API key in response`() {
            val settings = UserAiSettingsEntity(user = user, apiKey = "sk-ant-api03-secret-key", modelId = "claude-sonnet-4-20250514")
            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userAiSettingsFacade.findByUserId(user.id) } returns settings

            val result = service.get(auth0Sub)

            assertTrue(result.apiKeyHint.startsWith("sk-ant-a"))
            assertTrue(result.apiKeyHint.contains("*"))
        }

        @Test
        fun `should throw AiNotConfiguredException when user does not exist`() {
            every { userFacade.findByAuth0Sub(auth0Sub) } returns null

            assertThrows<AiNotConfiguredException> {
                service.get(auth0Sub)
            }
        }

        @Test
        fun `should throw AiNotConfiguredException when user has no settings`() {
            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userAiSettingsFacade.findByUserId(user.id) } returns null

            assertThrows<AiNotConfiguredException> {
                service.get(auth0Sub)
            }
        }
    }

    @Nested
    inner class Save {
        @Test
        fun `should create new settings for user without existing settings`() {
            val request = SaveAiSettingsRequest(apiKey = "sk-new-key", modelId = "claude-sonnet-4-20250514")
            every { userFacade.findOrCreate(auth0Sub) } returns user
            every { userAiSettingsFacade.findByUserId(user.id) } returns null
            every { userAiSettingsFacade.save(any()) } answers { firstArg() }

            val result = service.save(auth0Sub, request)

            assertEquals("claude-sonnet-4-20250514", result.modelId)
            verify { userAiSettingsFacade.save(any()) }
        }

        @Test
        fun `should update existing settings`() {
            val existing = UserAiSettingsEntity(user = user, apiKey = "sk-old-key", modelId = "old-model")
            val request = SaveAiSettingsRequest(apiKey = "sk-new-key", modelId = "new-model")
            every { userFacade.findOrCreate(auth0Sub) } returns user
            every { userAiSettingsFacade.findByUserId(user.id) } returns existing
            every { userAiSettingsFacade.save(existing) } returns existing

            val result = service.save(auth0Sub, request)

            assertEquals("new-model", result.modelId)
            assertEquals("sk-new-key", existing.apiKey)
        }
    }

    @Nested
    inner class ResolveForUser {
        @Test
        fun `should return entity when settings exist`() {
            val settings = UserAiSettingsEntity(user = user, apiKey = "sk-key", modelId = "model")
            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userAiSettingsFacade.findByUserId(user.id) } returns settings

            val result = service.resolveForUser(auth0Sub)

            assertEquals(settings, result)
        }

        @Test
        fun `should throw AiNotConfiguredException when user not found`() {
            every { userFacade.findByAuth0Sub(auth0Sub) } returns null

            assertThrows<AiNotConfiguredException> {
                service.resolveForUser(auth0Sub)
            }
        }

        @Test
        fun `should throw AiNotConfiguredException when settings not found`() {
            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userAiSettingsFacade.findByUserId(user.id) } returns null

            assertThrows<AiNotConfiguredException> {
                service.resolveForUser(auth0Sub)
            }
        }
    }
}
