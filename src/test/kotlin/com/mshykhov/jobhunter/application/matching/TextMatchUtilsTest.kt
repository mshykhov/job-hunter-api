package com.mshykhov.jobhunter.application.matching

/**
 * Unit tests for TextMatchUtils (pure logic, no dependencies).
 *
 * == buildSearchText ==
 * - should combine title, company, description, location, salary into lowercase
 * - should skip null fields (company, location, salary can be null)
 * - should return lowercase text
 *
 * == containsWord ==
 * - should match exact word with word boundaries
 * - should not match substring within another word (e.g., "go" should not match "google")
 * - should match case-insensitively
 * - should match word at start of text
 * - should match word at end of text
 * - should handle hyphenated words (e.g., "full-stack" as single token)
 * - should handle special regex characters in keyword
 *
 * == containsSubstring ==
 * - should match substring anywhere in text
 * - should match case-insensitively
 * - should match partial word (e.g., "java" matches "javascript")
 * - should return false when not found
 *
 * == @ParameterizedTest ==
 * - containsWord: @CsvSource with word boundary edge cases
 * - containsSubstring: @CsvSource with various match positions
 */
class TextMatchUtilsTest
