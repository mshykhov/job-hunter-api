package com.mshykhov.jobhunter.infrastructure.matching

import jakarta.validation.Validation
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class MatchingPropertiesTest {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `should accept default configuration`() {
        val violations = validator.validate(MatchingProperties())

        assertTrue(violations.isEmpty())
    }

    @Test
    fun `should reject a batch size of zero`() {
        val violations = validator.validate(MatchingProperties(batchSize = 0))

        assertTrue(violations.any { it.propertyPath.toString() == "batchSize" })
    }

    @Test
    fun `should reject a max attempts of zero`() {
        val violations = validator.validate(MatchingProperties(maxAttempts = 0))

        assertTrue(violations.any { it.propertyPath.toString() == "maxAttempts" })
    }
}
