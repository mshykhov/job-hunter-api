package com.mshykhov.jobhunter.application.matching

import com.mshykhov.jobhunter.support.TestFixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class TextMatchUtilsTest {
    @Nested
    inner class BuildSearchText {
        @Test
        fun `should combine all fields into lowercase text`() {
            val job =
                TestFixtures.jobEntity(
                    title = "Senior Developer",
                    company = "TechCorp",
                    description = "Great role",
                    location = "Kyiv",
                    salary = "5000 USD",
                )
            val result = TextMatchUtils.buildSearchText(job)

            assertTrue(result.contains("senior developer"))
            assertTrue(result.contains("techcorp"))
            assertTrue(result.contains("great role"))
            assertTrue(result.contains("kyiv"))
            assertTrue(result.contains("5000 usd"))
        }

        @Test
        fun `should skip null fields`() {
            val job =
                TestFixtures.jobEntity(
                    title = "Developer",
                    company = null,
                    description = "Role",
                    location = null,
                    salary = null,
                )
            val result = TextMatchUtils.buildSearchText(job)

            assertEquals("developer role", result)
        }

        @Test
        fun `should return lowercase text`() {
            val job = TestFixtures.jobEntity(title = "UPPER CASE TITLE", description = "MiXeD CaSe")
            val result = TextMatchUtils.buildSearchText(job)

            assertFalse(result.contains("UPPER"))
            assertTrue(result.contains("upper case title"))
            assertTrue(result.contains("mixed case"))
        }
    }

    @Nested
    inner class ContainsWord {
        @ParameterizedTest
        @CsvSource(
            "kotlin developer position, kotlin, true",
            "java and kotlin developer, kotlin, true",
            "looking for kotlin, kotlin, true",
            "google engineering team, go, false",
            "golang developer wanted, go, false",
            "use go language, go, true",
            "javascript developer, java, false",
            "java developer needed, java, true",
        )
        fun `should match word boundaries correctly`(
            text: String,
            word: String,
            expected: Boolean,
        ) {
            assertEquals(expected, TextMatchUtils.containsWord(text, word))
        }

        @Test
        fun `should lowercase keyword before matching`() {
            assertTrue(TextMatchUtils.containsWord("kotlin developer", "KOTLIN"))
            assertTrue(TextMatchUtils.containsWord("kotlin developer", "Kotlin"))
        }

        @Test
        fun `should handle hyphenated words as single token`() {
            assertTrue(TextMatchUtils.containsWord("full-stack developer", "full-stack"))
            assertFalse(TextMatchUtils.containsWord("full-stack developer", "full"))
            assertFalse(TextMatchUtils.containsWord("full-stack developer", "stack"))
        }

        @Test
        fun `should handle special regex characters in keyword`() {
            assertTrue(TextMatchUtils.containsWord("c++ developer needed", "c++"))
            assertTrue(TextMatchUtils.containsWord("use .net framework", ".net"))
        }
    }

    @Nested
    inner class ContainsSubstring {
        @ParameterizedTest
        @CsvSource(
            "javascript developer, java, true",
            "kotlin spring boot, spring, true",
            "senior developer, develop, true",
            "kotlin developer, python, false",
            "frontend engineer, backend, false",
        )
        fun `should match substring anywhere in text`(
            text: String,
            keyword: String,
            expected: Boolean,
        ) {
            assertEquals(expected, TextMatchUtils.containsSubstring(text, keyword))
        }

        @Test
        fun `should lowercase keyword before matching`() {
            assertTrue(TextMatchUtils.containsSubstring("kotlin developer", "KOTLIN"))
            assertTrue(TextMatchUtils.containsSubstring("kotlin developer", "Kotlin"))
        }
    }
}
