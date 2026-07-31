package com.mshykhov.jobhunter.application.ai

import com.mshykhov.jobhunter.application.settings.AiProvider
import com.mshykhov.jobhunter.application.user.UserRepository
import com.mshykhov.jobhunter.support.AbstractIntegrationTest
import com.mshykhov.jobhunter.support.TestFixtures
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import kotlin.test.assertEquals

class UserAiProviderFacadeIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var userAiProviderFacade: UserAiProviderFacade

    @Autowired
    lateinit var userAiProviderRepository: UserAiProviderRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Nested
    inner class FindByUserId {
        @Test
        fun `should return providers ordered by priority ascending`() {
            val user = userRepository.save(TestFixtures.userEntity())
            userAiProviderFacade.saveAll(
                listOf(
                    TestFixtures.userAiProviderEntity(user = user, priority = 2, provider = AiProvider.GEMINI),
                    TestFixtures.userAiProviderEntity(user = user, priority = 1, provider = AiProvider.OPENAI),
                ),
            )

            val result = userAiProviderFacade.findByUserId(user.id)

            assertEquals(listOf(AiProvider.OPENAI, AiProvider.GEMINI), result.map { it.provider })
        }
    }

    @Nested
    inner class UniqueProviderPerUser {
        @Test
        fun `should reject a duplicate provider for the same user`() {
            val user = userRepository.save(TestFixtures.userEntity())
            userAiProviderRepository.saveAndFlush(
                TestFixtures.userAiProviderEntity(user = user, priority = 1, provider = AiProvider.OPENAI),
            )

            assertThrows<DataIntegrityViolationException> {
                userAiProviderRepository.saveAndFlush(
                    TestFixtures.userAiProviderEntity(user = user, priority = 2, provider = AiProvider.OPENAI),
                )
            }
        }
    }

    @Nested
    inner class ApiKeyEncryption {
        @Test
        fun `should round-trip the encrypted api key`() {
            val user = userRepository.save(TestFixtures.userEntity())
            val plainApiKey = "sk-test-round-trip-key"
            userAiProviderFacade.saveAll(
                listOf(TestFixtures.userAiProviderEntity(user = user, provider = AiProvider.OPENAI, apiKey = plainApiKey)),
            )

            val result = userAiProviderFacade.findByUserIdAndProvider(user.id, AiProvider.OPENAI)

            assertEquals(plainApiKey, result?.apiKey)
        }
    }
}
