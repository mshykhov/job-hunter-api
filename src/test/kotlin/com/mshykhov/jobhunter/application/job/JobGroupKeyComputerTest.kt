package com.mshykhov.jobhunter.application.job

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class JobGroupKeyComputerTest {
    @Nested
    inner class Compute {
        @Test
        fun `should produce consistent hash for same inputs`() {
            val first = JobGroupKeyComputer.compute("Senior Kotlin Developer", "TechCorp")
            val second = JobGroupKeyComputer.compute("Senior Kotlin Developer", "TechCorp")

            assertEquals(first, second)
        }

        @Test
        fun `should be case insensitive`() {
            val lower = JobGroupKeyComputer.compute("senior kotlin developer", "techcorp")
            val upper = JobGroupKeyComputer.compute("SENIOR KOTLIN DEVELOPER", "TECHCORP")
            val mixed = JobGroupKeyComputer.compute("Senior Kotlin Developer", "TechCorp")

            assertEquals(lower, upper)
            assertEquals(lower, mixed)
        }

        @Test
        fun `should trim whitespace from title and company`() {
            val trimmed = JobGroupKeyComputer.compute("Senior Kotlin Developer", "TechCorp")
            val padded = JobGroupKeyComputer.compute("  Senior Kotlin Developer  ", "  TechCorp  ")

            assertEquals(trimmed, padded)
        }

        @Test
        fun `should treat null company as empty string`() {
            val withNull = JobGroupKeyComputer.compute("Senior Kotlin Developer", null)
            val withEmpty = JobGroupKeyComputer.compute("Senior Kotlin Developer", "")

            assertEquals(withNull, withEmpty)
        }

        @Test
        fun `should differentiate different titles`() {
            val kotlin = JobGroupKeyComputer.compute("Senior Kotlin Developer", "TechCorp")
            val java = JobGroupKeyComputer.compute("Senior Java Developer", "TechCorp")

            assertNotEquals(kotlin, java)
        }

        @Test
        fun `should differentiate different companies`() {
            val techCorp = JobGroupKeyComputer.compute("Senior Kotlin Developer", "TechCorp")
            val otherCorp = JobGroupKeyComputer.compute("Senior Kotlin Developer", "OtherCorp")

            assertNotEquals(techCorp, otherCorp)
        }

        @Test
        fun `should differentiate null company from non-null company`() {
            val withCompany = JobGroupKeyComputer.compute("Senior Kotlin Developer", "TechCorp")
            val withoutCompany = JobGroupKeyComputer.compute("Senior Kotlin Developer", null)

            assertNotEquals(withCompany, withoutCompany)
        }

        @Test
        fun `should produce 32-character hex string`() {
            val result = JobGroupKeyComputer.compute("Senior Kotlin Developer", "TechCorp")

            assertEquals(32, result.length)
            assert(result.all { it in '0'..'9' || it in 'a'..'f' }) {
                "Expected hex string, got: $result"
            }
        }

        @Test
        fun `should handle empty title`() {
            val result = JobGroupKeyComputer.compute("", null)

            assertEquals(32, result.length)
        }

        @Test
        fun `should handle special characters in title`() {
            val result = JobGroupKeyComputer.compute("C++ / C# Developer (Senior)", "Company & Co.")

            assertEquals(32, result.length)
        }

        @Test
        fun `should handle unicode characters`() {
            val result = JobGroupKeyComputer.compute("Розробник Kotlin", "Компанія")

            assertEquals(32, result.length)
        }

        @Test
        fun `should collapse inner whitespace`() {
            val singleSpace = JobGroupKeyComputer.compute("Senior Kotlin Developer", null)
            val doubleSpace = JobGroupKeyComputer.compute("Senior  Kotlin  Developer", null)

            assertEquals(singleSpace, doubleSpace)
        }
    }
}
