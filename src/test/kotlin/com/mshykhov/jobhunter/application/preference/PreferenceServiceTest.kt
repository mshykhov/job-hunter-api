package com.mshykhov.jobhunter.application.preference

import com.mshykhov.jobhunter.application.ai.AboutOptimizer
import com.mshykhov.jobhunter.application.ai.ChatClientFactory
import com.mshykhov.jobhunter.application.ai.PreferenceNormalizer
import com.mshykhov.jobhunter.application.ai.UserAiSettingsEntity
import com.mshykhov.jobhunter.application.ai.UserAiSettingsService
import com.mshykhov.jobhunter.application.common.NotFoundException
import com.mshykhov.jobhunter.application.user.UserFacade
import com.mshykhov.jobhunter.infrastructure.document.DocumentParser
import com.mshykhov.jobhunter.support.TestFixtures
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.ai.chat.client.ChatClient
import kotlin.test.assertEquals

class PreferenceServiceTest {
    private val userFacade = mockk<UserFacade>()
    private val userPreferenceFacade = mockk<UserPreferenceFacade>()
    private val preferenceNormalizer = mockk<PreferenceNormalizer>()
    private val aboutOptimizer = mockk<AboutOptimizer>()
    private val userAiSettingsService = mockk<UserAiSettingsService>()
    private val chatClientFactory = mockk<ChatClientFactory>()
    private val documentParser = mockk<DocumentParser>()

    private val service =
        PreferenceService(
            userFacade,
            userPreferenceFacade,
            preferenceNormalizer,
            aboutOptimizer,
            userAiSettingsService,
            chatClientFactory,
            documentParser,
        )

    @Nested
    inner class OptimizeAbout {
        private val auth0Sub = "auth0|test-user"
        private val user = TestFixtures.userEntity(auth0Sub)
        private val chatClient = mockk<ChatClient>()
        private val aiSettings = mockk<UserAiSettingsEntity>()

        @Test
        fun `should optimize about and save result`() {
            val preference = TestFixtures.userPreferenceEntity(user = user, about = "raw about text")
            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userPreferenceFacade.findByUserId(user.id) } returns preference
            every { userAiSettingsService.resolveForUser(auth0Sub) } returns aiSettings
            every { chatClientFactory.createForUser(aiSettings) } returns chatClient
            every { aboutOptimizer.optimize("raw about text", chatClient) } returns "optimized about"
            every { userPreferenceFacade.save(any()) } answers { firstArg() }

            val result = service.optimizeAbout(auth0Sub)

            assertEquals("optimized about", result.about)
            verify { userPreferenceFacade.save(match { it.about == "optimized about" }) }
        }

        @Test
        fun `should throw when user not found`() {
            every { userFacade.findByAuth0Sub(auth0Sub) } returns null

            assertThrows<NotFoundException> {
                service.optimizeAbout(auth0Sub)
            }
        }

        @Test
        fun `should throw when preferences not found`() {
            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userPreferenceFacade.findByUserId(user.id) } returns null

            assertThrows<NotFoundException> {
                service.optimizeAbout(auth0Sub)
            }
        }

        @Test
        fun `should throw when about is empty`() {
            val preference = TestFixtures.userPreferenceEntity(user = user, about = null)
            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userPreferenceFacade.findByUserId(user.id) } returns preference

            assertThrows<NotFoundException> {
                service.optimizeAbout(auth0Sub)
            }
        }
    }
}
