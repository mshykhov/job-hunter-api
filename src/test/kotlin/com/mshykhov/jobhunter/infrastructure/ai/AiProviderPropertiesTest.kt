package com.mshykhov.jobhunter.infrastructure.ai

import com.mshykhov.jobhunter.application.settings.AiProvider
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AiProviderPropertiesTest {
    @Test
    fun `should strip a trailing v1 without a slash from the codex base url`() {
        val properties = AiProviderProperties(codexBaseUrl = "http://cli-proxy-api-prd.svc:8317/v1")

        assertEquals("http://cli-proxy-api-prd.svc:8317", properties.baseUrlFor(AiProvider.CODEX))
    }

    @Test
    fun `should strip a trailing v1 with a slash from the codex base url`() {
        val properties = AiProviderProperties(codexBaseUrl = "http://cli-proxy-api-prd.svc:8317/v1/")

        assertEquals("http://cli-proxy-api-prd.svc:8317", properties.baseUrlFor(AiProvider.CODEX))
    }

    @Test
    fun `should leave a base url without a trailing v1 unchanged`() {
        val properties = AiProviderProperties(codexBaseUrl = "http://cli-proxy-api-prd.svc:8317")

        assertEquals("http://cli-proxy-api-prd.svc:8317", properties.baseUrlFor(AiProvider.CODEX))
    }

    @Test
    fun `should strip a trailing v1 from the gemini base url too`() {
        val properties = AiProviderProperties(geminiBaseUrl = "https://generativelanguage.googleapis.com/v1")

        assertEquals("https://generativelanguage.googleapis.com", properties.baseUrlFor(AiProvider.GEMINI))
    }

    @Test
    fun `should return null for openai regardless of configuration`() {
        val properties = AiProviderProperties(codexBaseUrl = "http://host/v1")

        assertNull(properties.baseUrlFor(AiProvider.OPENAI))
    }

    @Test
    fun `should return null when the base url is blank`() {
        val properties = AiProviderProperties()

        assertNull(properties.baseUrlFor(AiProvider.CODEX))
    }
}
