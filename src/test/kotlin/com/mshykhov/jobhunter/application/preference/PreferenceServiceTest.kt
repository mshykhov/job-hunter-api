package com.mshykhov.jobhunter.application.preference

/**
 * Unit tests for PreferenceService (MockK-based).
 *
 * == get ==
 * - should return preference response for existing user
 * - should throw NotFoundException when user has no preferences
 *
 * == saveAbout ==
 * - should save about text to existing preference
 * - should create preference if not exists (findOrCreate)
 * - should create user if not exists
 * - should overwrite previous about text
 *
 * == saveAboutFromFile ==
 * - should extract text from PDF and save
 * - should extract text from DOCX and save
 * - should throw IllegalArgumentException for unsupported content type
 * - should throw IllegalArgumentException when content type is null
 *
 * == generatePreferences ==
 * - should throw NotFoundException when about is null/empty
 * - should throw AiNotConfiguredException when no AI settings
 * - should call PreferenceNormalizer with about text and chat client
 * - should return normalized preferences from AI
 *
 * == saveSearch ==
 * - should apply search preferences via applyTo pattern
 * - should create preference if not exists
 *
 * == saveMatching ==
 * - should apply matching preferences via applyTo pattern
 * - should create preference if not exists
 *
 * == saveTelegram ==
 * - should apply telegram preferences via applyTo pattern
 * - should create preference if not exists
 */
class PreferenceServiceTest
