package com.mshykhov.jobhunter.application.settings

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AiModelTest {
    @Test
    fun `every model id is unique`() {
        val ids = AiModel.entries.map { it.id }

        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `every provider has at least one model`() {
        val providersWithModels = AiModel.entries.map { it.provider }.toSet()

        assertEquals(AiProvider.entries.toSet(), providersWithModels)
    }

    @Test
    fun `only Codex does not require an api key`() {
        val providersWithoutApiKey = AiProvider.entries.filterNot { it.requiresApiKey }

        assertEquals(listOf(AiProvider.CODEX), providersWithoutApiKey)
    }
}
