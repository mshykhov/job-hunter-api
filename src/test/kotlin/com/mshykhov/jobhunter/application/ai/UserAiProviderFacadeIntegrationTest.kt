package com.mshykhov.jobhunter.application.ai

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
                    TestFixtures.userAiProviderEntity(user = user, priority = 2, provider = "ANTHROPIC"),
                    TestFixtures.userAiProviderEntity(user = user, priority = 1, provider = "OPENAI"),
                ),
            )

            val result = userAiProviderFacade.findByUserId(user.id)

            assertEquals(listOf("OPENAI", "ANTHROPIC"), result.map { it.provider })
        }
    }

    @Nested
    inner class UniqueProviderPerUser {
        @Test
        fun `should reject a duplicate provider for the same user`() {
            val user = userRepository.save(TestFixtures.userEntity())
            userAiProviderRepository.saveAndFlush(
                TestFixtures.userAiProviderEntity(user = user, priority = 1, provider = "OPENAI"),
            )

            assertThrows<DataIntegrityViolationException> {
                userAiProviderRepository.saveAndFlush(
                    TestFixtures.userAiProviderEntity(user = user, priority = 2, provider = "OPENAI"),
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
                listOf(TestFixtures.userAiProviderEntity(user = user, provider = "OPENAI", apiKey = plainApiKey)),
            )

            val result = userAiProviderFacade.findByUserIdAndProvider(user.id, "OPENAI")

            assertEquals(plainApiKey, result?.apiKey)
        }
    }
}
