package com.mshykhov.jobhunter.application.ai.dto

import org.junit.jupiter.api.Test
import org.springframework.ai.converter.BeanOutputConverter
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NormalizedPreferencesTest {
    private val converter = BeanOutputConverter(NormalizedPreferences::class.java)

    @Test
    fun `should describe categories as plain strings in the generated AI schema`() {
        val format = converter.format

        // Value classes leak into the victools-generated schema as {"value": "..."} objects,
        // which makes the model return objects Jackson then cannot map back to strings.
        assertTrue(format.contains("\"categories\""))
        assertFalse(format.contains("\"value\""), "categories must be plain strings in the schema, got: $format")
    }

    @Test
    fun `should parse model response with plain string categories`() {
        val result =
            converter.convert(
                """{"categories":["java","kotlin"],"excludedKeywords":[],"locations":[],"remoteOnly":true,"disabledSources":[]}""",
            )!!

        assertEquals(listOf("java", "kotlin"), result.categories.map { it.toString() })
        assertTrue(result.remoteOnly)
    }
}
