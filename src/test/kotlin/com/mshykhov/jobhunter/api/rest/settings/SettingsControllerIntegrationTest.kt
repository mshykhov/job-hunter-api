package com.mshykhov.jobhunter.api.rest.settings

import com.mshykhov.jobhunter.support.AbstractIntegrationTest

/**
 * Integration tests for SettingsController (/settings).
 *
 * == GET /settings/ai-providers ==
 * - should return list of AI providers with models
 * - should include recommended models
 * - response should be stable (static catalog)
 *
 * == GET /settings/ai ==
 * - should return 422 (AI_NOT_CONFIGURED) when user has no AI settings
 * - should return saved AI settings after PUT
 * - should mask API key in response (not return raw key)
 *
 * == PUT /settings/ai ==
 * - should save AI settings (apiKey + modelId)
 * - should update existing AI settings (overwrite)
 * - should return 400 when apiKey is blank
 * - should return 400 when modelId is blank
 * - should return 400 when body is missing
 * - should encrypt API key (verify via GET that key is masked)
 *
 * == GET /settings/outreach ==
 * - should return default (empty) outreach settings for new user
 * - should return saved outreach settings after PUT
 *
 * == PUT /settings/outreach ==
 * - should save outreach settings with default prompts
 * - should save source-specific config per JobSource
 * - should overwrite previous outreach settings
 * - should accept null prompts (clears custom prompts)
 * - should accept empty sourceConfig
 *
 * == POST /settings/outreach/test/cover-letter ==
 * - should return 422 (AI_NOT_CONFIGURED) when no AI settings
 * - should return 404 when no jobs exist for given source
 * (AI generation tests are better suited for service-level mocking)
 *
 * == POST /settings/outreach/test/recruiter-message ==
 * - should return 422 (AI_NOT_CONFIGURED) when no AI settings
 * - should return 404 when no jobs exist for given source
 */
class SettingsControllerIntegrationTest : AbstractIntegrationTest()
