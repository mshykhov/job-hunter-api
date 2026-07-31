package com.mshykhov.jobhunter.application.ai

import com.mshykhov.jobhunter.api.rest.settings.dto.SaveAiProviderChainEntryRequest
import com.mshykhov.jobhunter.api.rest.settings.dto.SaveAiProviderChainRequest
import com.mshykhov.jobhunter.api.rest.settings.dto.SaveAiSettingsRequest
import com.mshykhov.jobhunter.application.common.AiNotConfiguredException
import com.mshykhov.jobhunter.application.common.ValidationException
import com.mshykhov.jobhunter.application.settings.AiProvider
import com.mshykhov.jobhunter.application.user.UserFacade
import com.mshykhov.jobhunter.support.TestFixtures
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserAiProviderServiceTest {
    private val userFacade = mockk<UserFacade>()
    private val userAiProviderFacade = mockk<UserAiProviderFacade>()
    private val service = UserAiProviderService(userFacade, userAiProviderFacade)

    private val auth0Sub = "auth0|test-user"
    private val user = TestFixtures.userEntity(auth0Sub = auth0Sub)

    @Nested
    inner class Get {
        @Test
        fun `should return the priority-1 row`() {
            val primary = TestFixtures.userAiProviderEntity(user = user, priority = 1, modelId = "gpt-4o-mini")
            val secondary = TestFixtures.userAiProviderEntity(user = user, priority = 2, provider = AiProvider.GEMINI)
            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userAiProviderFacade.findByUserId(user.id) } returns listOf(primary, secondary)

            val result = service.get(auth0Sub)

            assertEquals("gpt-4o-mini", result.modelId)
        }

        @Test
        fun `should throw AiNotConfiguredException when user does not exist`() {
            every { userFacade.findByAuth0Sub(auth0Sub) } returns null

            assertThrows<AiNotConfiguredException> { service.get(auth0Sub) }
        }

        @Test
        fun `should throw AiNotConfiguredException when no priority-1 row exists`() {
            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userAiProviderFacade.findByUserId(user.id) } returns emptyList()

            assertThrows<AiNotConfiguredException> { service.get(auth0Sub) }
        }

        @Test
        fun `should skip a disabled priority-1 row and return the first enabled one`() {
            val disabledPrimary = TestFixtures.userAiProviderEntity(user = user, priority = 1, enabled = false)
            val enabledSecondary =
                TestFixtures.userAiProviderEntity(user = user, priority = 2, provider = AiProvider.GEMINI, modelId = "gemini-2.5-flash-lite")
            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userAiProviderFacade.findByUserId(user.id) } returns listOf(disabledPrimary, enabledSecondary)

            val result = service.get(auth0Sub)

            assertEquals("gemini-2.5-flash-lite", result.modelId)
        }
    }

    @Nested
    inner class Save {
        @Test
        fun `should create a priority-1 OPENAI row for a user with no providers`() {
            val request = SaveAiSettingsRequest(apiKey = "sk-new-key", modelId = "gpt-4o-mini")
            val savedSlot = slot<List<UserAiProviderEntity>>()
            every { userFacade.findOrCreate(auth0Sub) } returns user
            every { userAiProviderFacade.findByUserId(user.id) } returns emptyList()
            every { userAiProviderFacade.saveAll(capture(savedSlot)) } answers { savedSlot.captured }

            val result = service.save(auth0Sub, request)

            assertEquals("gpt-4o-mini", result.modelId)
            assertEquals(1, savedSlot.captured[0].priority)
            assertEquals(AiProvider.OPENAI, savedSlot.captured[0].provider)
        }

        @Test
        fun `should update the existing priority-1 row without touching its provider`() {
            val existing =
                TestFixtures.userAiProviderEntity(user = user, priority = 1, provider = AiProvider.CODEX, modelId = "gpt-5.6-luna")
            val request = SaveAiSettingsRequest(apiKey = "", modelId = "gpt-5.6-sol")
            every { userFacade.findOrCreate(auth0Sub) } returns user
            every { userAiProviderFacade.findByUserId(user.id) } returns listOf(existing)
            every { userAiProviderFacade.saveAll(listOf(existing)) } returns listOf(existing)

            val result = service.save(auth0Sub, request)

            assertEquals("gpt-5.6-sol", result.modelId)
            assertEquals(AiProvider.CODEX, existing.provider)
        }

        @Test
        fun `should throw ValidationException when apiKey is blank and no row exists`() {
            val request = SaveAiSettingsRequest(apiKey = "", modelId = "gpt-4o-mini")
            every { userFacade.findOrCreate(auth0Sub) } returns user
            every { userAiProviderFacade.findByUserId(user.id) } returns emptyList()

            assertThrows<ValidationException> { service.save(auth0Sub, request) }
        }

        @Test
        fun `should reject a model whose provider differs from the target row's provider`() {
            val existing =
                TestFixtures.userAiProviderEntity(user = user, priority = 1, provider = AiProvider.CODEX, modelId = "gpt-5.6-luna")
            val request = SaveAiSettingsRequest(apiKey = null, modelId = "gpt-5-mini")
            every { userFacade.findOrCreate(auth0Sub) } returns user
            every { userAiProviderFacade.findByUserId(user.id) } returns listOf(existing)

            assertThrows<ValidationException> { service.save(auth0Sub, request) }
        }

        @Test
        fun `should update the first enabled row rather than whatever sits at priority 1`() {
            val disabledPrimary =
                TestFixtures.userAiProviderEntity(user = user, priority = 1, provider = AiProvider.CODEX, enabled = false)
            val enabledSecondary =
                TestFixtures.userAiProviderEntity(user = user, priority = 2, provider = AiProvider.OPENAI, modelId = "gpt-4o-mini")
            val request = SaveAiSettingsRequest(apiKey = "sk-updated-key", modelId = "gpt-5-mini")
            every { userFacade.findOrCreate(auth0Sub) } returns user
            every { userAiProviderFacade.findByUserId(user.id) } returns listOf(disabledPrimary, enabledSecondary)
            every { userAiProviderFacade.saveAll(listOf(enabledSecondary)) } returns listOf(enabledSecondary)

            val result = service.save(auth0Sub, request)

            assertEquals("gpt-5-mini", result.modelId)
            assertEquals("gpt-4o-mini", disabledPrimary.modelId)
        }
    }

    @Nested
    inner class ChainFor {
        @Test
        fun `should delegate to the facade`() {
            val providers = listOf(TestFixtures.userAiProviderEntity(user = user))
            every { userAiProviderFacade.findByUserId(user.id) } returns providers

            assertEquals(providers, service.chainFor(user.id))
        }
    }

    @Nested
    inner class ResolvePrimary {
        @Test
        fun `should return the highest priority enabled provider`() {
            val disabledFirst = TestFixtures.userAiProviderEntity(user = user, priority = 1, enabled = false)
            val enabledSecond = TestFixtures.userAiProviderEntity(user = user, priority = 2, provider = AiProvider.GEMINI)
            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userAiProviderFacade.findByUserId(user.id) } returns listOf(disabledFirst, enabledSecond)

            val result = service.resolvePrimary(auth0Sub)

            assertEquals(enabledSecond, result)
        }

        @Test
        fun `should throw AiNotConfiguredException when user does not exist`() {
            every { userFacade.findByAuth0Sub(auth0Sub) } returns null

            assertThrows<AiNotConfiguredException> { service.resolvePrimary(auth0Sub) }
        }

        @Test
        fun `should throw AiNotConfiguredException when every provider is disabled`() {
            val disabled = TestFixtures.userAiProviderEntity(user = user, enabled = false)
            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userAiProviderFacade.findByUserId(user.id) } returns listOf(disabled)

            assertThrows<AiNotConfiguredException> { service.resolvePrimary(auth0Sub) }
        }
    }

    @Nested
    inner class GetChain {
        @Test
        fun `should return an empty chain when the user does not exist`() {
            every { userFacade.findByAuth0Sub(auth0Sub) } returns null

            val result = service.getChain(auth0Sub)

            assertEquals(emptyList(), result.chain)
        }

        @Test
        fun `should return the configured chain ordered by priority`() {
            val second = TestFixtures.userAiProviderEntity(user = user, priority = 2, provider = AiProvider.GEMINI)
            val first = TestFixtures.userAiProviderEntity(user = user, priority = 1, provider = AiProvider.OPENAI)
            every { userFacade.findByAuth0Sub(auth0Sub) } returns user
            every { userAiProviderFacade.findByUserId(user.id) } returns listOf(first, second)

            val result = service.getChain(auth0Sub)

            assertEquals(listOf(AiProvider.OPENAI, AiProvider.GEMINI), result.chain.map { it.provider })
        }
    }

    @Nested
    inner class ReplaceChain {
        @Test
        fun `should delete the existing chain and persist the validated replacement`() {
            val request =
                SaveAiProviderChainRequest(
                    chain =
                    listOf(
                        SaveAiProviderChainEntryRequest(priority = 1, provider = AiProvider.CODEX, modelId = "gpt-5.6-luna"),
                        SaveAiProviderChainEntryRequest(
                            priority = 2,
                            provider = AiProvider.OPENAI,
                            modelId = "gpt-4o-mini",
                            apiKey = "sk-new-chain-key",
                        ),
                    ),
                )
            val savedSlot = slot<List<UserAiProviderEntity>>()
            every { userFacade.findOrCreate(auth0Sub) } returns user
            every { userAiProviderFacade.findByUserId(user.id) } returns emptyList()
            every { userAiProviderFacade.deleteAll(user.id) } just Runs
            every { userAiProviderFacade.saveAll(capture(savedSlot)) } answers { savedSlot.captured }

            val result = service.replaceChain(auth0Sub, request)

            verify { userAiProviderFacade.deleteAll(user.id) }
            assertEquals(listOf(AiProvider.CODEX, AiProvider.OPENAI), savedSlot.captured.map { it.provider })
            assertEquals(listOf(AiProvider.CODEX, AiProvider.OPENAI), result.chain.map { it.provider })
        }

        @Test
        fun `should reject priorities that are not a contiguous ascending sequence starting at 1`() {
            val request =
                SaveAiProviderChainRequest(
                    chain =
                    listOf(
                        SaveAiProviderChainEntryRequest(priority = 1, provider = AiProvider.CODEX, modelId = "gpt-5.6-luna"),
                        SaveAiProviderChainEntryRequest(
                            priority = 3,
                            provider = AiProvider.OPENAI,
                            modelId = "gpt-4o-mini",
                            apiKey = "sk-key",
                        ),
                    ),
                )
            every { userFacade.findOrCreate(auth0Sub) } returns user
            every { userAiProviderFacade.findByUserId(user.id) } returns emptyList()

            assertThrows<ValidationException> { service.replaceChain(auth0Sub, request) }
        }

        @Test
        fun `should reject a provider listed twice`() {
            val request =
                SaveAiProviderChainRequest(
                    chain =
                    listOf(
                        SaveAiProviderChainEntryRequest(
                            priority = 1,
                            provider = AiProvider.OPENAI,
                            modelId = "gpt-4o-mini",
                            apiKey = "sk-key-1",
                        ),
                        SaveAiProviderChainEntryRequest(
                            priority = 2,
                            provider = AiProvider.OPENAI,
                            modelId = "gpt-4o",
                            apiKey = "sk-key-2",
                        ),
                    ),
                )
            every { userFacade.findOrCreate(auth0Sub) } returns user
            every { userAiProviderFacade.findByUserId(user.id) } returns emptyList()

            assertThrows<ValidationException> { service.replaceChain(auth0Sub, request) }
        }

        @Test
        fun `should accept a CODEX entry without an api key`() {
            val request =
                SaveAiProviderChainRequest(
                    chain = listOf(SaveAiProviderChainEntryRequest(priority = 1, provider = AiProvider.CODEX, modelId = "gpt-5.6-luna")),
                )
            val savedSlot = slot<List<UserAiProviderEntity>>()
            every { userFacade.findOrCreate(auth0Sub) } returns user
            every { userAiProviderFacade.findByUserId(user.id) } returns emptyList()
            every { userAiProviderFacade.deleteAll(user.id) } just Runs
            every { userAiProviderFacade.saveAll(capture(savedSlot)) } answers { savedSlot.captured }

            service.replaceChain(auth0Sub, request)

            assertTrue(savedSlot.captured[0].apiKey.isEmpty())
        }

        @Test
        fun `should reject a new OPENAI entry with a blank api key when no stored row exists`() {
            val request =
                SaveAiProviderChainRequest(
                    chain = listOf(SaveAiProviderChainEntryRequest(priority = 1, provider = AiProvider.OPENAI, modelId = "gpt-4o-mini")),
                )
            every { userFacade.findOrCreate(auth0Sub) } returns user
            every { userAiProviderFacade.findByUserId(user.id) } returns emptyList()

            assertThrows<ValidationException> { service.replaceChain(auth0Sub, request) }
        }

        @Test
        fun `should carry over the stored key when an existing entry omits it during a reorder`() {
            val storedOpenAi =
                TestFixtures.userAiProviderEntity(
                    user = user,
                    priority = 1,
                    provider = AiProvider.OPENAI,
                    apiKey = "sk-stored-key-123456",
                    modelId = "gpt-4o-mini",
                )
            val request =
                SaveAiProviderChainRequest(
                    chain =
                    listOf(
                        SaveAiProviderChainEntryRequest(priority = 1, provider = AiProvider.CODEX, modelId = "gpt-5.6-luna"),
                        SaveAiProviderChainEntryRequest(priority = 2, provider = AiProvider.OPENAI, modelId = "gpt-4o-mini"),
                    ),
                )
            val savedSlot = slot<List<UserAiProviderEntity>>()
            every { userFacade.findOrCreate(auth0Sub) } returns user
            every { userAiProviderFacade.findByUserId(user.id) } returns listOf(storedOpenAi)
            every { userAiProviderFacade.deleteAll(user.id) } just Runs
            every { userAiProviderFacade.saveAll(capture(savedSlot)) } answers { savedSlot.captured }

            service.replaceChain(auth0Sub, request)

            val openAiEntity = savedSlot.captured.first { it.provider == AiProvider.OPENAI }
            assertEquals("sk-stored-key-123456", openAiEntity.apiKey)
        }

        @Test
        fun `should replace the stored key when an entry supplies a new one`() {
            val storedOpenAi =
                TestFixtures.userAiProviderEntity(
                    user = user,
                    priority = 1,
                    provider = AiProvider.OPENAI,
                    apiKey = "sk-old-key-123456",
                    modelId = "gpt-4o-mini",
                )
            val request =
                SaveAiProviderChainRequest(
                    chain =
                    listOf(
                        SaveAiProviderChainEntryRequest(
                            priority = 1,
                            provider = AiProvider.OPENAI,
                            modelId = "gpt-4o-mini",
                            apiKey = "sk-new-key-123456",
                        ),
                    ),
                )
            val savedSlot = slot<List<UserAiProviderEntity>>()
            every { userFacade.findOrCreate(auth0Sub) } returns user
            every { userAiProviderFacade.findByUserId(user.id) } returns listOf(storedOpenAi)
            every { userAiProviderFacade.deleteAll(user.id) } just Runs
            every { userAiProviderFacade.saveAll(capture(savedSlot)) } answers { savedSlot.captured }

            service.replaceChain(auth0Sub, request)

            assertEquals("sk-new-key-123456", savedSlot.captured[0].apiKey)
        }
    }
}
