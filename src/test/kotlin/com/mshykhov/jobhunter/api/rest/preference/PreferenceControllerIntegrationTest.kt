package com.mshykhov.jobhunter.api.rest.preference

import com.mshykhov.jobhunter.support.AbstractIntegrationTest

/**
 * Integration tests for PreferenceController (/preferences).
 *
 * == GET /preferences ==
 * - should return default preferences for new user (auto-created)
 * - should return saved preferences after updates
 *
 * == PUT /preferences/about ==
 * - should save about text and return it
 * - should overwrite previous about text
 * - should return 400 when about is blank (@NotBlank)
 * - should return 400 when about is missing from body
 *
 * == PUT /preferences/about/file ==
 * - should extract text from PDF and save as about
 * - should extract text from DOCX and save as about
 * - should return 400 for unsupported file type (e.g. .txt, .jpg)
 * - should return 400 when no file provided
 *
 * == POST /preferences/generate ==
 * - should return 422 (AI_NOT_CONFIGURED) when user has no AI settings
 * - should return 404 when about is empty (nothing to normalize)
 * (AI-dependent tests are better suited for service-level mocking)
 *
 * == PUT /preferences/search ==
 * - should save search preferences (categories, locations, remoteOnly, disabledSources)
 * - should overwrite previous search preferences
 * - should accept empty lists as valid input
 * - should accept all JobSource values in disabledSources
 *
 * == PUT /preferences/matching ==
 * - should save matching preferences with valid weights (sum=100)
 * - should return 400 when weights don't sum to 100
 * - should return 400 when weight exceeds 100
 * - should return 400 when weight is negative
 * - should save with edge weights (100/0/0, 0/0/100, etc.)
 * - should save keywords, excluded keywords, seniority levels
 * - should save custom prompt
 * - should toggle matchWithAi flag
 *
 * == PUT /preferences/telegram ==
 * - should save telegram preferences (chatId, username, notifications)
 * - should accept null chatId and username
 * - should toggle notificationsEnabled
 *
 * == Cross-cutting ==
 * - should create user and preference on first write (findOrCreate pattern)
 * - subsequent reads should reflect latest saved data
 */
class PreferenceControllerIntegrationTest : AbstractIntegrationTest()
