package com.mshykhov.jobhunter.application.ai

/**
 * Unit tests for UserAiSettingsService (MockK-based).
 *
 * == get ==
 * - should return AI settings response for existing user
 * - should throw AiNotConfiguredException when user has no settings
 * - should throw AiNotConfiguredException when user doesn't exist
 * - should mask API key in response
 *
 * == save ==
 * - should create new settings for user without existing settings
 * - should update existing settings (apiKey + modelId)
 * - should create user if not exists (findOrCreate)
 * - should encrypt API key on save
 *
 * == resolveForUser ==
 * - should return entity when settings exist
 * - should throw AiNotConfiguredException when user not found
 * - should throw AiNotConfiguredException when settings not found
 */
class UserAiSettingsServiceTest
